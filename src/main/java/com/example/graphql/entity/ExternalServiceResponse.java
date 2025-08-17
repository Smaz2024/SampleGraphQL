package com.example.graphql.entity;

/**
 * Represents a generic response structure from an external service. This simple entity is used to
 * encapsulate the data received from a third-party API, typically a string payload, which can then
 * be processed further within the application.
 */
public class ExternalServiceResponse {
  private final String data;

  /**
   * Constructs a new ExternalServiceResponse with the provided data.
   *
   * @param data The string data received from the external service.
   */
  public ExternalServiceResponse(String data) {
    this.data = data;
  }

  /**
   * Retrieves the data received from the external service.
   *
   * @return The string data.
   */
  public String getData() {
    return data;
  }
}
