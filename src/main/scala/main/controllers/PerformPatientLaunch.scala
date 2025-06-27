package main.controllers

import com.drajer.bsa.dao.HealthcareSettingsDao
import com.drajer.bsa.ehr.service.EhrQueryService
import com.drajer.bsa.exceptions.{InvalidLaunchContext, InvalidNotification}
import com.drajer.bsa.model.{HealthcareSetting, KarProcessingData, NotificationContext, PatientLaunchContext}
import com.drajer.bsa.service.SubscriptionNotificationReceiver
import org.hl7.fhir.r4.model.{Bundle, CanonicalType, CodeType, Encounter, IntegerType, Meta, Parameters, Reference, Resource, ResourceType}
import org.hl7.fhir.r4.model.Bundle.{BundleType, HTTPVerb}
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.Value

import scala.jdk.CollectionConverters.*
import java.time.Instant
import java.util.{Date, UUID}
import scala.util.{Try, Failure, Success}

sealed trait PatientLaunchError

/** The HealthCareSettings were not found in the database */
case object HealthCareSettingsNotFound extends PatientLaunchError

/** Unknown exception occurred */
case object UnknownError extends PatientLaunchError

/** Cannot launch due to conflict (resource already exists) */
case object LaunchConflict extends PatientLaunchError

case object InvalidLaunchContext extends PatientLaunchError
case object InvalidNotification extends PatientLaunchError

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
): Either[PatientLaunchError, Unit] = {
  for {
    // Load the HealthCareSetting from the database
    healthcareSetting: HealthcareSetting <- getHealthcareSetting(launchContext.getFhirServerURL)
    // Construct the KarProcessingData DTO
    karProcessingData: KarProcessingData <- Right(createKarProcessingData(healthcareSetting, requestId))
    // Get the resource from the EHR service
    encounter: Encounter <- getEncounter(launchContext.getEncounterId, karProcessingData)
    // Create the notification bundle
    notificationBundle: Bundle <- Right(createNotificationBundle(
      fhirServerUrl = launchContext.getFhirServerURL,
      encounter = encounter,
      relaunch = false
    ))
    // Process the notification. No need to use the result for anything here
    _ <- processNotification(notificationBundle, launchContext, requestId, correlationId)
    // Return success if everything succeeded
  } yield()
}

private def getHealthcareSetting(url: String)(
  implicit hsDao: HealthcareSettingsDao
): Either[PatientLaunchError, HealthcareSetting] = {
  Try(hsDao.getHealthcareSettingByUrl(url)) match {
    case Failure(exception) => Left(UnknownError)
    case Success(null) => Left(HealthCareSettingsNotFound)
    case Success(hs) => Right(hs)
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
): Either[PatientLaunchError, Encounter] = {
  Try(ehrService.getResourceById(karProcessingData, ResourceType.Encounter.toString, encounterId, true)) match {
    case Success(resource: Encounter) => Right(resource)
    case Success(resource: Resource) => {
      logger.error(s"Expected Encounter resource but got ${resource.getResourceType} for encounterId: $encounterId")
      Left(UnknownError)
    }
    case Failure(exception) => Left(UnknownError)
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
): Either[PatientLaunchError, List[KarProcessingData]] = {
  Try(notificationReceiver.processNotification(notificationBundle, requestId, correlationId, launchContext)) match {
    case Success(result) => Right(result.asScala.toList)
    case Failure(exception: InvalidLaunchContext) => Left(InvalidLaunchContext)
    case Failure(exception: InvalidNotification) => Left(InvalidNotification)
    case Failure(exception) => Left(UnknownError)
  }
}