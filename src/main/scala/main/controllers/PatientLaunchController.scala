package main.controllers

import com.drajer.bsa.controller.PatientLaunchController
import com.drajer.bsa.dao.HealthcareSettingsDao
import com.drajer.bsa.ehr.service.EhrQueryService
import com.drajer.bsa.model.{HealthcareSetting, PatientLaunchContext}
import com.drajer.bsa.service.{HealthcareSettingsService, SubscriptionNotificationReceiver}
import com.drajer.bsa.utils.{OperationOutcomeUtil, StartupUtils}
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StringEscapeUtils
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.http.{HttpStatus, ResponseEntity}
import org.springframework.web.bind.annotation.{CrossOrigin, PostMapping, RequestBody}

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
  private val X_REQUEST_ID = "X-Request-ID"

  /**
   * Launches a patient instance for processing.
   *
   * @param launchContext the context containing details for launching the patient
   * @param request       the HTTP request
   * @param response      the HTTP response
   * @return a ResponseEntity indicating the result of the launch operation
   */
  def launchPatient(@RequestBody launchContext: PatientLaunchContext,
                    request: HttpServletRequest,
                    response: HttpServletResponse)
  : ResponseEntity[AnyRef] = {
    logPatientLaunch(launchContext, request)

    // Awaiting app startup
    if (!StartupUtils.hasAppStarted)
      return PatientLaunchController.appNotStartedResponse()

    val requestId: String = request.getHeader(X_REQUEST_ID)

    // Missing X-Request-ID header
    if (StringUtils.isEmpty(requestId)) {
      logger.error(s"Request ID is missing in the request header: $X_REQUEST_ID")

      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(OperationOutcomeUtil.createErrorOperationOutcome(
          "No Request Id set in the header. Add X-Request-ID parameter for request tracking."
        ))
    }

    // Perform the launch and handle the result
    performPatientLaunch(requestId, launchContext) match {
      case PatientLaunchSuccess() =>
        logger.info("Patient launch was successful for patientId: {}, encounterId: {}, requestId: {}",
          StringEscapeUtils.escapeJava(launchContext.getPatientId),
          StringEscapeUtils.escapeJava(launchContext.getEncounterId),
          StringEscapeUtils.escapeJava(request.getHeader(X_REQUEST_ID))
        )

        ResponseEntity.ok().body(OperationOutcomeUtil.createSuccessOperationOutcome(
          "Patient Instance launched for processing successfully"
        ))

      case HealthCareSettingsNotFound =>
        logger.error("Healthcare setting not found for URL: {}", launchContext.getFhirServerURL)
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
          OperationOutcomeUtil.createErrorOperationOutcome(
            s"Healthcare setting not found for URL: ${launchContext.getFhirServerURL}")
        )
    }
  }

  private def logPatientLaunch(launchContext: PatientLaunchContext,
                               request: HttpServletRequest): Unit = {
    logger.info(
      "Patient launch request received for fhirServerUrl: {}, patientId: {}, encounterId: {}, ehrLaunchContext: {}, requestId: {},  throttleContext: {}",
      StringEscapeUtils.escapeJava(launchContext.getFhirServerURL),
      StringEscapeUtils.escapeJava(launchContext.getPatientId),
      StringEscapeUtils.escapeJava(launchContext.getEncounterId),
      launchContext.getEhrLaunchContext.size,
      StringEscapeUtils.escapeJava(request.getHeader(X_REQUEST_ID)),
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
