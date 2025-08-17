package com.example.graphql.scalar;

import graphql.language.StringValue;
import graphql.schema.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * Registers a custom GraphQL scalar named 'DateTime' that maps between GraphQL strings and Java's
 * LocalDateTime using ISO-8601 formatting.
 *
 * <p>This configuration ensures that fields declared as 'DateTime' in the GraphQL schema are
 * correctly parsed and serialized to/from Java LocalDateTime objects.
 *
 * <p>The scalar handles:
 *
 * <ul>
 *   <li><b>Serialization</b>: Converting LocalDateTime to ISO-8601 String for GraphQL responses
 *   <li><b>Parsing Input</b>: Converting GraphQL string input to LocalDateTime
 *   <li><b>Parsing Literal</b>: Converting hardcoded values in GraphQL queries
 * </ul>
 */
@Configuration
public class DateTimeScalar {

  /**
   * Configures a GraphQL scalar type named 'DateTime' and registers it with the GraphQL runtime.
   *
   * @return RuntimeWiringConfigurer that registers the custom scalar
   */
  @Bean
  public RuntimeWiringConfigurer runtimeWiringConfigurer() {
    return wiringBuilder ->
        wiringBuilder.scalar(
            GraphQLScalarType.newScalar()
                .name("DateTime")
                .description(
                    "A custom scalar that handles Java 8 LocalDateTime using ISO-8601 format.")
                .coercing(
                    new Coercing<LocalDateTime, String>() {

                      /**
                       * Converts a Java LocalDateTime to an ISO-8601 formatted String for inclusion
                       * in the GraphQL response.
                       *
                       * @param dataFetcherResult the result from a data fetcher
                       * @return a string representation of the LocalDateTime
                       */
                      @Override
                      public String serialize(Object dataFetcherResult) {
                        if (dataFetcherResult instanceof LocalDateTime) {
                          return ((LocalDateTime) dataFetcherResult)
                              .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        }
                        throw new CoercingSerializeException("Expected a LocalDateTime object.");
                      }

                      /**
                       * Parses a GraphQL input variable (e.g., JSON payload) into LocalDateTime.
                       *
                       * @param input the input value
                       * @return a parsed LocalDateTime object
                       */
                      @Override
                      public LocalDateTime parseValue(Object input) {
                        if (input instanceof String) {
                          try {
                            return LocalDateTime.parse(
                                (String) input, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                          } catch (DateTimeParseException e) {
                            throw new CoercingParseValueException(
                                "Invalid DateTime format: " + input, e);
                          }
                        }
                        throw new CoercingParseValueException("Expected a String input.");
                      }

                      /**
                       * Parses hardcoded literals directly written into the GraphQL query. e.g.,
                       * mutation { createEvent(startTime: "2025-07-06T12:00:00") }
                       *
                       * @param input the GraphQL literal
                       * @return a parsed LocalDateTime object
                       */
                      @Override
                      public LocalDateTime parseLiteral(Object input) {
                        if (input instanceof StringValue) {
                          try {
                            return LocalDateTime.parse(
                                ((StringValue) input).getValue(),
                                DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                          } catch (DateTimeParseException e) {
                            throw new CoercingParseLiteralException(
                                "Invalid DateTime format in literal: " + input, e);
                          }
                        }
                        throw new CoercingParseLiteralException("Expected a StringValue literal.");
                      }
                    })
                .build());
  }
}
