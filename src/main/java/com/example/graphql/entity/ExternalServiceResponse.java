package com.example.graphql.entity;

/**
 * Represents a generic response structure from an external service. This simple entity encapsulates
 * the string payload received from a third-party API, which can be processed further within the
 * application.
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

    public String getData() {
        return data;
    }
}
