package com.example.graphql.entity;

import java.util.List;

/**
 * Represents a combined response from multiple external services. This entity is designed to
 * encapsulate data fetched from different sources (e.g., Service A and Service B) and can also
 * carry a list of errors if any issues occurred during the data retrieval process. It's
 * particularly useful in scenarios where data aggregation from microservices is required, and
 * partial success or error reporting is necessary.
 */
public class CombinedDataResponse {
  private final String serviceAData;
  private final String serviceBData;
  private final List<String> errors;

  /**
   * Constructs a new CombinedDataResponse with data from two services and an optional list of
   * errors.
   *
   * @param serviceAData The data retrieved from Service A.
   * @param serviceBData The data retrieved from Service B.
   * @param errors A list of error messages, or {@code null} if no errors occurred.
   */
  public CombinedDataResponse(String serviceAData, String serviceBData, List<String> errors) {
    this.serviceAData = serviceAData;
    this.serviceBData = serviceBData;
    this.errors = errors;
  }

  /**
   * Constructs a new CombinedDataResponse with data from two services and no errors. This is a
   * convenience constructor for successful responses.
   *
   * @param serviceAData The data retrieved from Service A.
   * @param serviceBData The data retrieved from Service B.
   */
  public CombinedDataResponse(String serviceAData, String serviceBData) {
    this(serviceAData, serviceBData, null);
  }

  /**
   * Retrieves the data from Service A.
   *
   * @return The data from Service A.
   */
  public String getServiceAData() {
    return serviceAData;
  }

  /**
   * Retrieves the data from Service B.
   *
   * @return The data from Service B.
   */
  public String getServiceBData() {
    return serviceBData;
  }

  /**
   * Retrieves the list of errors.
   *
   * @return A list of error messages, or {@code null} if no errors.
   */
  public List<String> getErrors() {
    return errors;
  }

  /**
   * Checks if the response contains any errors.
   *
   * @return {@code true} if there are errors (list is not null and not empty), {@code false}
   *     otherwise.
   */
  public boolean hasErrors() {
    return errors != null && !errors.isEmpty();
  }
}
