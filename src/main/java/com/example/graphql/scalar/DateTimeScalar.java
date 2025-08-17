package com.example.graphql.scalar;

import graphql.language.StringValue;
import graphql.schema.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/** Custom GraphQL scalar for mapping Java LocalDateTime <-> ISO-8601 formatted strings. */
@Configuration
public class DateTimeScalar {

    @Bean
    public RuntimeWiringConfigurer dateTimeScalarConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarType.newScalar()
                .name("DateTime")
                .description("A custom scalar that handles Java LocalDateTime using ISO-8601 format.")
                .coercing(new Coercing<LocalDateTime, String>() {

                    @Override
                    public String serialize(Object dataFetcherResult) {
                        if (dataFetcherResult == null) {
                            return null;
                        }
                        if (dataFetcherResult instanceof LocalDateTime) {
                            return ((LocalDateTime) dataFetcherResult).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        }
                        throw new CoercingSerializeException("Expected a LocalDateTime object.");
                    }

                    @Override
                    public LocalDateTime parseValue(Object input) {
                        if (input == null) {
                            return null;
                        }
                        if (input instanceof String) {
                            try {
                                return LocalDateTime.parse((String) input, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            } catch (DateTimeParseException e) {
                                throw new CoercingParseValueException("Invalid ISO-8601 DateTime format: " + input, e);
                            }
                        }
                        throw new CoercingParseValueException("Expected an ISO-8601 DateTime string.");
                    }

                    @Override
                    public LocalDateTime parseLiteral(Object input) {
                        if (input instanceof StringValue) {
                            try {
                                return LocalDateTime.parse(
                                        ((StringValue) input).getValue(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            } catch (DateTimeParseException e) {
                                throw new CoercingParseLiteralException(
                                        "Invalid ISO-8601 DateTime literal: " + input, e);
                            }
                        }
                        throw new CoercingParseLiteralException("Expected a StringValue literal.");
                    }
                })
                .build());
    }
}
