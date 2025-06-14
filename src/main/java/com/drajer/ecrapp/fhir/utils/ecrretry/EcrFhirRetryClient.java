package com.drajer.ecrapp.fhir.utils.ecrretry;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IGetPage;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IRead;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import com.drajer.eca.model.EventTypes;
import com.drajer.ecrapp.fhir.utils.FHIRRetryTemplate;
import com.drajer.ecrapp.fhir.utils.RetryableException;
import com.drajer.sof.utils.FhirClient;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class EcrFhirRetryClient extends FhirClient {

  private FHIRRetryTemplate fhirRetryTemplate;

  public EcrFhirRetryClient(
      IGenericClient parent,
      FHIRRetryTemplate fhirRetryTemplate,
      String requestId,
      EventTypes.QueryType type
  ) {
    super(parent, requestId, type);
    this.fhirRetryTemplate = fhirRetryTemplate;
  }

  public FHIRRetryTemplate getRetryTemplate() {
    return fhirRetryTemplate;
  }

  public IRead read() {
    interceptor.reset();
    return new EcrFhirRetryableRead(client.read(), this);
  }

  public IGetPage loadPage() {
    return new EcrFhirRetryablePage(client.loadPage(), this);
  }

  public <T extends IBaseBundle> IUntypedQuery<T> search() {
    return new EcrFhirRetryableSearch<>(client.search(), this);
  }

  public RuntimeException handleException(final Exception e, final String methodName) {
    int httpStatusCode = 0;

    if (e instanceof BaseServerResponseException) {
      httpStatusCode = ((BaseServerResponseException) e).getStatusCode();
    }
    return new RetryableException(e, httpStatusCode, methodName);
  }
}
