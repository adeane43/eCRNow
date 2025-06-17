package main.controllers

import com.drajer.bsa.dao.HealthcareSettingsDao
import com.drajer.bsa.ehr.service.EhrQueryService
import com.drajer.bsa.model.{HealthcareSetting, KarProcessingData, NotificationContext, PatientLaunchContext}
import com.drajer.bsa.service.SubscriptionNotificationReceiver
import com.drajer.bsa.utils.StartupUtils
import main.util.{Result, Success, Failure}
import org.apache.commons.text.StringEscapeUtils
import org.hl7.fhir.r4.model.{Bundle, CanonicalType, CodeType, IntegerType, Meta, Parameters, Reference, Resource, ResourceType}
import org.hl7.fhir.r4.model.Bundle.{BundleType, HTTPVerb}
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException

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
case object Conflict extends PatientLaunchResult

/** The token refresh threshold value for refreshing access tokens */
@Value("${token.refresh.threshold:25}")
private val tokenRefreshThreshold = null
private def logger: Logger = LoggerFactory.getLogger(getClass)

def performPatientLaunch(requestId: String, launchContext: PatientLaunchContext)(
  implicit hsDao: HealthcareSettingsDao,
  ehrService: EhrQueryService,
  notificationReceiver: SubscriptionNotificationReceiver,
): PatientLaunchResult = {
  val result = for {
    // Load the HealthCareSetting from the database
    healthcareSetting <- getHealthcareSetting(launchContext.getFhirServerURL)
    // Construct the KarProcessingData DTO containing
    karProcessingData <- Success(createKarProcessingData(launchContext, healthcareSetting, requestId))
    resource <- getResource(launchContext, karProcessingData)
    notificationBundle <- Success(createNotificationBundle(
      fhirServerUrl = launchContext.getFhirServerURL,
      resource = resource,
      relaunch = false
    ))
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

private def createKarProcessingData(context: PatientLaunchContext, healthcareSetting: HealthcareSetting, requestId: String)
: KarProcessingData = {
  // TODO: Make it a case class
  val kd = new KarProcessingData
  kd.setHealthcareSetting(healthcareSetting)
  kd.setNotificationContext(new NotificationContext)
  kd.getNotificationContext.setxRequestId(requestId)
  kd.setTokenRefreshThreshold(tokenRefreshThreshold)
  kd
}

private def getResource(launchContext: PatientLaunchContext, karProcessingData: KarProcessingData)(
  implicit ehrService: EhrQueryService
): Result[PatientLaunchResult, Resource] = {
  Try(ehrService.getResourceById(karProcessingData, ResourceType.Encounter.toString, launchContext.getEncounterId, true)) match {
    case TryFailure(exception) => Failure(UnknownError)
    case TrySuccess(resource: Resource) => Success(resource)
  }
}

private def createNotificationBundle(fhirServerUrl: String, resource: Resource, relaunch: Boolean)
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

  nb.addEntry(new Bundle.BundleEntryComponent().setResource(resource))
}