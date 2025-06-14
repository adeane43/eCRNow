package com.drajer.ecrapp.fhir.utils.ecrretry;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.SearchStyleEnum;
import ca.uhn.fhir.rest.api.SearchTotalModeEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.ISort;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.text.StringEscapeUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

@SuppressWarnings({"rawtypes", "unchecked"})
public class EcrFhirRetryableSearch<T extends IBaseBundle> implements IQuery<T>, IUntypedQuery<T> {
  private IQuery<T> query;
  private final EcrFhirRetryClient client;
  private IUntypedQuery<T> untypedQuery;
  private static final Logger logger = LoggerFactory.getLogger(EcrFhirRetryableSearch.class);
  private static String url;

  public EcrFhirRetryableSearch(final IUntypedQuery<T> untypedQuery, final EcrFhirRetryClient client) {
    this.untypedQuery = untypedQuery;
    this.client = client;
  }

  public EcrFhirRetryableSearch(final IQuery<T> query, final EcrFhirRetryClient client) {
    this.query = query;
    this.client = client;
  }

  @Override
  public IQuery<T> andLogRequestAndResponse(boolean theLogRequestAndResponse) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T> cacheControl(CacheControlDirective theCacheControlDirective) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T> elementsSubset(String... theElements) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T> encoded(EncodingEnum theEncoding) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T> encodedJson() {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T> encodedXml() {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T> withAdditionalHeader(String theHeaderName, String theHeaderValue) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public T execute() {
    return client
            .getRetryTemplate()
            .execute((context) -> {
                      try {
                        logger.info(
                                "Retrying FHIR search url {}. Count {} ",
                                StringEscapeUtils.escapeJava(url),
                                context.getRetryCount());
                        return query.execute();
                      } catch (final Exception e) {
                        throw client.handleException(e, HttpMethod.GET.name());
                      }
                    },
                    null);
  }

  @Override
  public IQuery<T> preferResponseType(Class theType) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T> preferResponseTypes(List theTypes) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T> prettyPrint() {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T> summaryMode(SummaryEnum theSummary) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T> accept(String theHeaderValue) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T>forAllResources() {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T>forResource(String theResourceName) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T> forResource(Class theClass) {
    query = ((IUntypedQuery) query).forResource(theClass);
    return new EcrFhirRetryableSearch(query, client);
  }

  @Override
  public IQuery<T> byUrl(String theSearchUrl) {
    this.url = theSearchUrl;
    return untypedQuery.byUrl(theSearchUrl);
  }

  @Override
  public IQuery<T>and(ICriterion theCriterion) {
    query = query.and(theCriterion);
    return new EcrFhirRetryableSearch(query, client);
  }

  @Override
  public IQuery<T>count(int theCount) {
    query = query.count(theCount);
    return new EcrFhirRetryableSearch(query, client);
  }

  @Override
  public IQuery<T>offset(int i) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T>include(Include theInclude) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T>lastUpdated(DateRangeParam theLastUpdated) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T>limitTo(int theLimitTo) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T>returnBundle(Class theClass) {
    return new EcrFhirRetryableSearch(query.returnBundle(theClass), client);
  }

  @Override
  public IQuery<T>totalMode(SearchTotalModeEnum theTotalMode) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T>revInclude(Include theIncludeTarget) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public ISort sort() {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T>sort(SortSpec theSortSpec) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T>usingStyle(SearchStyleEnum theStyle) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T>where(ICriterion theCriterion) {
    query = query.where(theCriterion);
    return new EcrFhirRetryableSearch(query, client);
  }

  @Override
  public IQuery<T>withAnyProfile(Collection theProfileUris) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T>withIdAndCompartment(String theResourceId, String theCompartmentName) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T>withProfile(String theProfileUri) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T>withSecurity(String theSystem, String theCode) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T>withTag(String theSystem, String theCode) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T> where(Map<String, List<IQueryParameterType>> theCriterion) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }

  @Override
  public IQuery<T> whereMap(Map<String, List<String>> theRawMap) {
    throw new NotImplementedOperationException("The requested operation is not implemented");
  }
}