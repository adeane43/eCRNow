package com.drajer.ecrapp.fhir.utils.ecrretry;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.gclient.*;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

public class EcrFhirRetryableRead implements IRead, IReadTyped<IBaseResource>, IReadExecutable<IBaseResource> {

  private IRead readParent;
  private IReadTyped<IBaseResource> readTypedParent;
  private IReadExecutable<IBaseResource> readExecutableParent;
  private final EcrFhirRetryClient client;

  private static final Logger logger = LoggerFactory.getLogger(EcrFhirRetryableRead.class);

  public EcrFhirRetryableRead(IRead read, EcrFhirRetryClient client) {
    super();
    this.readParent = read;
    this.client = client;
  }

  public EcrFhirRetryableRead(IReadTyped<IBaseResource> readTyped, EcrFhirRetryClient client) {
    super();
    this.readTypedParent = readTyped;
    this.client = client;
  }

  public EcrFhirRetryableRead(IReadExecutable<IBaseResource> readExecutable, EcrFhirRetryClient client) {
    super();
    this.readExecutableParent = readExecutable;
    this.client = client;
  }

  @Override
  public <T extends IBaseResource> IReadTyped<T> resource(Class<T> theResourceType) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IReadTyped<IBaseResource> resource(String theResourceType) {
    return new EcrFhirRetryableRead(readParent.resource(theResourceType), this.client);
  }

  @Override
  public IReadExecutable<IBaseResource> withId(String theId) {
    return new EcrFhirRetryableRead(readTypedParent.withId(theId), this.client);
  }

  @Override
  public IReadExecutable<IBaseResource> withIdAndVersion(String theId, String theVersion) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IReadExecutable<IBaseResource> withId(Long theId) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IReadExecutable<IBaseResource> withId(IIdType theId) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IReadExecutable<IBaseResource> withUrl(String theUrl) {
    return new EcrFhirRetryableRead(readTypedParent.withId(theUrl), this.client);
  }

  @Override
  public IReadExecutable<IBaseResource> withUrl(IIdType theUrl) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IReadExecutable<IBaseResource> andLogRequestAndResponse(boolean theLogRequestAndResponse) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IReadExecutable<IBaseResource> cacheControl(CacheControlDirective theCacheControlDirective) {

    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IReadExecutable<IBaseResource> elementsSubset(String... theElements) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IReadExecutable<IBaseResource> encoded(EncodingEnum theEncoding) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IReadExecutable<IBaseResource> encodedJson() {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IReadExecutable<IBaseResource> encodedXml() {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IReadExecutable<IBaseResource> withAdditionalHeader(String theHeaderName, String theHeaderValue) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IBaseResource execute() {
    return client
        .getRetryTemplate()
        .execute(
            retryContext -> {
              try {
                logger.info("Retrying FHIR read. Count: {} ", retryContext.getRetryCount());
                return readExecutableParent.execute();
              } catch (final Exception ex) {
                throw client.handleException(ex, HttpMethod.GET.name());
              }
            },
            null);
  }

  @Override
  public IReadExecutable<IBaseResource> preferResponseType(Class theType) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IReadExecutable<IBaseResource> preferResponseTypes(List theTypes) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IReadExecutable<IBaseResource> prettyPrint() {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IReadExecutable<IBaseResource> summaryMode(SummaryEnum theSummary) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IReadExecutable<IBaseResource> accept(String theHeaderValue) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IReadIfNoneMatch<IBaseResource> ifVersionMatches(String theVersion) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }
}
