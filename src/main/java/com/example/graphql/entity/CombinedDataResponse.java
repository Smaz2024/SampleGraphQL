package com.example.graphql.entity;

import java.util.Collections;
import java.util.List;

/**
 * Represents a combined response from multiple external services. This entity encapsulates data
 * fetched from different sources (e.g., Service A and Service B) and may also carry a list of
 * errors if issues occurred during data retrieval. It supports scenarios where data aggregation
 * from microservices is required and partial success/error reporting is needed.
 */
public class CombinedDataResponse {
    /**
     * DTO for combining multiple data sources in a single GraphQL response.
     */
    private final String serviceAData;
    private final String serviceBData;
    private final List<String> errors;

    /**
     * Constructs a new CombinedDataResponse with data from two services and an optional list of
     * errors.
     *
     * @param serviceAData The data retrieved from Service A.
     * @param serviceBData The data retrieved from Service B.
     * @param errors A list of error messages, or {@code null}/empty if no errors occurred.
     */
    public CombinedDataResponse(String serviceAData, String serviceBData, List<String> errors) {
        this.serviceAData = serviceAData;
        this.serviceBData = serviceBData;
        this.errors = errors == null ? Collections.emptyList() : errors;
    }

    /**
     * Convenience constructor for successful responses without errors.
     *
     * @param serviceAData The data retrieved from Service A.
     * @param serviceBData The data retrieved from Service B.
     */
    public CombinedDataResponse(String serviceAData, String serviceBData) {
        this(serviceAData, serviceBData, Collections.emptyList());
    }

    public String getServiceAData() {
        return serviceAData;
    }

    public String getServiceBData() {
        return serviceBData;
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Checks if the response contains any errors.
     *
     * @return {@code true} if errors exist, otherwise {@code false}.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
