package main.controllers

import com.drajer.bsa.controller.PatientLaunchController
import com.drajer.bsa.dao.HealthcareSettingsDao
import com.drajer.bsa.ehr.service.EhrQueryService
import com.drajer.bsa.model.{HealthcareSetting, PatientLaunchContext}
import com.drajer.bsa.service.{HealthcareSettingsService, SubscriptionNotificationReceiver}
import com.drajer.bsa.utils.{OperationOutcomeUtil, StartupUtils}
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.commons.text.StringEscapeUtils
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.http.{HttpStatus, ResponseEntity}
import org.springframework.web.bind.annotation.{CrossOrigin, PostMapping, RequestBody}

@CrossOrigin
@PostMapping(value = Array("/api/launchPatient"))
class PatientLaunchController {
  @Autowired
  val hsDao: HealthcareSettingsDao = null

  @Autowired
  val ehrService: EhrQueryService = null

  @Autowired
  val notificationReceiver: SubscriptionNotificationReceiver = null

  /** The token refresh threshold value for refreshing access tokens */
  @Value("${token.refresh.threshold:25}")
  private val tokenRefreshThreshold = null

  private val logger = LoggerFactory.getLogger(classOf[PatientLaunchController])

  private val FHIR_VERSION = "fhirVersion"
  private val X_REQUEST_ID = "X-Request-ID"

  def launchPatient(@RequestBody launchContext: PatientLaunchContext,
                    request: HttpServletRequest,
                    response: HttpServletResponse)
  : ResponseEntity[AnyRef] = {
    logPatientLaunch(launchContext, request)

    if (!StartupUtils.hasAppStarted) {
      logger.error(" Unable to launch the patient instance due to application startup delay")

      ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
        OperationOutcomeUtil.createErrorOperationOutcome(
          "Unable to launch Patient Instance since the app has not started yet, wait till "
            + StartupUtils.getPatientLaunchInstanceTime.toString
            + " for the application to startup and launch patients")
      )
    }

    val hs = hsDao.getHealthcareSettingByUrl(launchContext.getFhirServerURL)

    // If the healthcare setting exists// If the healthcare setting exists
    if (hs == null) ResponseEntity.status(HttpStatus.OK)
      .body(OperationOutcomeUtil.createSuccessOperationOutcome(
        "Patient Instance launched for processing successfully"
      ))
    )


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
