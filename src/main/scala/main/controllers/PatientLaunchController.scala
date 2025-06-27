package main.controllers

import com.drajer.bsa.dao.HealthcareSettingsDao
import com.drajer.bsa.ehr.service.EhrQueryService
import com.drajer.bsa.model.{PatientLaunchContext}
import com.drajer.bsa.service.{SubscriptionNotificationReceiver}
import com.drajer.bsa.utils.{OperationOutcomeUtil, StartupUtils}
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import main.constants.Headers
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StringEscapeUtils
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.http.{HttpStatus, ResponseEntity}
import org.springframework.web.bind.annotation.{CrossOrigin, PostMapping, RequestBody, RequestHeader}

@CrossOrigin
@PostMapping(value = Array("/api/launchPatient"))
class PatientLaunchController {
  @Autowired
  implicit val hsDao: HealthcareSettingsDao = null

  @Autowired
  implicit val ehrService: EhrQueryService = null

  @Autowired
  implicit val notificationReceiver: SubscriptionNotificationReceiver = null

  /** The token refresh threshold value for refreshing access tokens */
  @Value("${token.refresh.threshold:25}")
  private val tokenRefreshThreshold = null

  private implicit val logger: Logger = LoggerFactory.getLogger(classOf[PatientLaunchController])

  private val FHIR_VERSION = "fhirVersion"

  /**
   * Launches a patient instance for processing.
   *
   * @param launchContext the context containing details for launching the patient
   * @param request       the HTTP request
   * @param response      the HTTP response
   * @return a ResponseEntity indicating the result of the launch operation
   */
  @CrossOrigin
  @PostMapping(value = Array("/api/launchPatient"))
  def launchPatient
  (
    @RequestBody launchContext: PatientLaunchContext,
    @RequestHeader(Headers.X_REQUEST_ID) requestIdHeaderValue: String,
    @RequestHeader(Headers.X_CORRELATION_ID) correlationIdHeaderValue: String,
    request: HttpServletRequest,
    response: HttpServletResponse
  ): ResponseEntity[AnyRef] = {

    // Awaiting app startup
    if (!StartupUtils.hasAppStarted)
      return PatientLaunchController.appNotStartedResponse()

    val requestId = StringEscapeUtils.escapeJava(requestIdHeaderValue)
    val correlationId = StringEscapeUtils.escapeJava(correlationIdHeaderValue)

    logPatientLaunch(launchContext, requestId)

    // Missing X-Request-ID header
    if (StringUtils.isEmpty(requestId)) {
      logger.error(s"Request ID is missing in the request header: ${Headers.X_REQUEST_ID}")

      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(OperationOutcomeUtil.createErrorOperationOutcome(
          "No Request Id set in the header. Add X-Request-ID parameter for request tracking."
        ))
    }

    // Perform the launch and handle the result
    performPatientLaunch(launchContext, requestId, correlationId) match {
      // Success
      case Right(_) =>
        logger.info("Patient launch was successful for patientId: {}, encounterId: {}, requestId: {}",
          StringEscapeUtils.escapeJava(launchContext.getPatientId),
          StringEscapeUtils.escapeJava(launchContext.getEncounterId),
          requestId
        )

        ResponseEntity.ok().body(OperationOutcomeUtil.createSuccessOperationOutcome(
          "Patient Instance launched for processing successfully"
        ))

      // Failure
      case Left(error: PatientLaunchError) => error match {
        case HealthCareSettingsNotFound =>
          logger.error("Healthcare setting not found for URL: {}", launchContext.getFhirServerURL)

          ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            OperationOutcomeUtil.createErrorOperationOutcome(
              s"Healthcare setting not found for URL: ${launchContext.getFhirServerURL}")
          )

        case LaunchConflict => ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
          .body(OperationOutcomeUtil.createErrorOperationOutcome(
            "Unable to launch Patient Instance - Patient encounter already exists in the system"
          ))

        // TODO: App doesn't currently handle these cases explicitly
        case InvalidNotification => genericError()
        case InvalidLaunchContext => genericError()
        case UnknownError => genericError()
      }
    }
  }

  private def genericError(): ResponseEntity[Object] = {
    ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
      OperationOutcomeUtil.createErrorOperationOutcome(
        s"Unable to launch Patient Instance due to an unexpected error")
    )
  }

  private def logPatientLaunch(launchContext: PatientLaunchContext, requestId: String): Unit = {
    logger.info(
      "Patient launch request received for fhirServerUrl: {}, patientId: {}, encounterId: {}, ehrLaunchContext: {}, requestId: {},  throttleContext: {}",
      StringEscapeUtils.escapeJava(launchContext.getFhirServerURL),
      StringEscapeUtils.escapeJava(launchContext.getPatientId),
      StringEscapeUtils.escapeJava(launchContext.getEncounterId),
      launchContext.getEhrLaunchContext.size,
      requestId,
      launchContext.getThrottleContext)
  }
}

object PatientLaunchController {
  private def appNotStartedResponse()(implicit logger: Logger): ResponseEntity[Object] = {
    ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
      OperationOutcomeUtil.createErrorOperationOutcome(
        "Unable to launch Patient Instance since the app has not started yet, wait till "
          + StartupUtils.getPatientLaunchInstanceTime.toString
          + " for the application to startup and launch patients")
    )
  }
}
