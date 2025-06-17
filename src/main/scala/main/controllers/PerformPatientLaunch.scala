package main.controllers

import com.drajer.bsa.dao.HealthcareSettingsDao
import com.drajer.bsa.ehr.service.EhrQueryService
import com.drajer.bsa.exceptions.{InvalidLaunchContext, InvalidNotification}
import com.drajer.bsa.model.{HealthcareSetting, KarProcessingData, NotificationContext, PatientLaunchContext}
import com.drajer.bsa.service.SubscriptionNotificationReceiver
import main.util.{Failure, Result, Success}
import org.hl7.fhir.r4.model.{Bundle, CanonicalType, CodeType, Encounter, IntegerType, Meta, Parameters, Reference, Resource, ResourceType}
import org.hl7.fhir.r4.model.Bundle.{BundleType, HTTPVerb}
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.Value

import scala.jdk.CollectionConverters.*
import java.time.Instant
import java.util.{Date, UUID}
import scala.util.{Try, Failure as TryFailure, Success as TrySuccess}

sealed trait PatientLaunchResult

/** Represents a successful patient launch operation */
case object PatientLaunchSuccess extends PatientLaunchResult

/** The HealthCareSettings were not found in the database */
case object HealthCareSettingsNotFound extends PatientLaunchResult

/** Unknown exception occurred */
case object UnknownError extends PatientLaunchResult

/** Cannot launch due to conflict (resource already exists) */
case object LaunchConflict extends PatientLaunchResult

case object InvalidLaunchContext extends PatientLaunchResult
case object InvalidNotification extends PatientLaunchResult

/** The token refresh threshold value for refreshing access tokens */
@Value("${token.refresh.threshold:25}")
private val tokenRefreshThreshold = null
private def logger: Logger = LoggerFactory.getLogger(getClass)

/**
 * Performs the patient launch operation
 * @param requestId The request ID for tracking the operation
 * @param launchContext The launch context containing details for launching the patient encounter
 * @param hsDao DAO for loading healthcare settings
 * @param ehrService DAO for querying EHR resources
 * @param notificationReceiver DAO for
 * @return
 */
def performPatientLaunch(launchContext: PatientLaunchContext, requestId: String, correlationId: String)(
  implicit hsDao: HealthcareSettingsDao,
  ehrService: EhrQueryService,
  notificationReceiver: SubscriptionNotificationReceiver,
): PatientLaunchResult = {
  val result = for {
    // Load the HealthCareSetting from the database
    healthcareSetting <- getHealthcareSetting(launchContext.getFhirServerURL)
    // Construct the KarProcessingData DTO
    karProcessingData <- Success(createKarProcessingData(healthcareSetting, requestId))
    // Get the resource from the EHR service
    encounter <- getEncounter(launchContext.getEncounterId, karProcessingData)
    // Create the notification bundle
    notificationBundle <- Success(createNotificationBundle(
      fhirServerUrl = launchContext.getFhirServerURL,
      encounter = encounter,
      relaunch = false
    ))
    // Process the notification. No need to use the result for anything here
    _ <- processNotification(notificationBundle, launchContext, requestId, correlationId)
    // Return success if everything succeeded
  } yield PatientLaunchSuccess

  result.merge
}

private def getHealthcareSetting(url: String)(
  implicit hsDao: HealthcareSettingsDao
): Result[PatientLaunchResult, HealthcareSetting] = {
  Try(hsDao.getHealthcareSettingByUrl(url)) match {
    case TryFailure(exception) => Failure(UnknownError)
    case TrySuccess(null) => Failure(HealthCareSettingsNotFound)
    case TrySuccess(hs) => Success(hs)
  }
}

private def createKarProcessingData(healthcareSetting: HealthcareSetting, requestId: String)
: KarProcessingData = {
  // TODO: Make it a case class
  val kd = new KarProcessingData
  kd.setHealthcareSetting(healthcareSetting)
  kd.setNotificationContext(new NotificationContext)
  kd.getNotificationContext.setxRequestId(requestId)
  kd.setTokenRefreshThreshold(tokenRefreshThreshold)
  kd
}

private def getEncounter(encounterId: String, karProcessingData: KarProcessingData)(
  implicit ehrService: EhrQueryService
): Result[PatientLaunchResult, Encounter] = {
  Try(ehrService.getResourceById(karProcessingData, ResourceType.Encounter.toString, encounterId, true)) match {
    case TrySuccess(resource: Encounter) => Success(resource)
    case TrySuccess(resource: Resource) => {
      logger.error(s"Expected Encounter resource but got ${resource.getResourceType} for encounterId: $encounterId")
      Failure(UnknownError)
    }
    case TryFailure(exception) => Failure(UnknownError)
  }
}

private def createNotificationBundle(fhirServerUrl: String, encounter: Encounter, relaunch: Boolean)
: Bundle = {
  val nb = new Bundle

  nb.setId("notification-full-resource")

  // Setup Meta// Setup Meta
  val met = new Meta
  met.setLastUpdated(Date.from(Instant.now))
  met.addProfile("http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-subscription-notification")
  nb.setMeta(met)

  // Setup other attributes// Setup other attributes
  nb.setType(BundleType.HISTORY)
  nb.setTimestamp(Date.from(Instant.now))

  // Add parameters// Add parameters
  val params = new Parameters
  val paramsId = UUID.randomUUID.toString
  params.setId(paramsId)
  val paramMeta = new Meta
  met.setLastUpdated(Date.from(Instant.now))
  met.addProfile("http://hl7.org/fhir/uv/subscriptions-backport/StructureDefinition/backport-subscriptionstatus")
  params.setMeta(paramMeta)

  // Add Subscription// Add Subscription
  if (!relaunch) {
    val subsRef = new Reference
    val url = fhirServerUrl + "/Subscription/encounter-start"
    subsRef.setReference(url)
    params.addParameter("subscription", subsRef)
    // Add topic
    val topicRef = new CanonicalType
    val topicUrl = "http://hl7.org/fhir/us/medmorph/SubscriptionTopic/encounter-start"
    topicRef.setValue(topicUrl)
    params.addParameter("topic", topicRef)
  }
  else {
    val subsRef = new Reference
    val url = fhirServerUrl + "/Subscription/encounter-modified"
    subsRef.setReference(url)
    params.addParameter("subscription", subsRef)
    // Add topic
    val topicRef = new CanonicalType
    val topicUrl = "http://hl7.org/fhir/us/medmorph/SubscriptionTopic/encounter-modified"
    topicRef.setValue(topicUrl)
    params.addParameter("topic", topicRef)
  }

  // Add Type and Status// Add Type and Status
  val ev = new CodeType
  ev.setValue("event-notification")
  params.addParameter("type", ev)
  val status = new CodeType
  status.setValue("active")
  params.addParameter("status", status)
  val it = new IntegerType
  it.setValue(1)
  params.addParameter("events-since-subscription-start", it)
  val ite = new IntegerType
  ite.setValue(1)
  params.addParameter("events-in-notification", ite)

  // Add Entry// Add Entry
  val bec = new Bundle.BundleEntryComponent
  bec.setResource(params)
  bec.setFullUrl(paramsId)
  val berc = new Bundle.BundleEntryRequestComponent
  berc.setMethod(HTTPVerb.GET)
  berc.setUrl(fhirServerUrl + "/Subscription/admission/$status")
  bec.setRequest(berc)
  val berpc = new Bundle.BundleEntryResponseComponent
  berpc.setStatus("200")
  nb.addEntry(bec)

  nb.addEntry(new Bundle.BundleEntryComponent().setResource(encounter))
}

private def processNotification(notificationBundle: Bundle, launchContext: PatientLaunchContext, requestId: String, correlationId: String)(
  implicit notificationReceiver: SubscriptionNotificationReceiver
): Result[PatientLaunchResult, List[KarProcessingData]] = {
  Try(notificationReceiver.processNotification(notificationBundle, requestId, correlationId, launchContext)) match {
    case TrySuccess(result) => Success(result.asScala.toList)
    case TryFailure(exception: InvalidLaunchContext) => Failure(InvalidLaunchContext)
    case TryFailure(exception: InvalidNotification) => Failure(InvalidNotification)
    case TryFailure(exception) => Failure(UnknownError)
  }
}