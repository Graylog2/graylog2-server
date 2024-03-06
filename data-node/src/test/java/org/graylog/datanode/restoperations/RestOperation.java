/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.datanode.restoperations;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.bson.assertions.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.HttpMethod;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

public abstract class RestOperation {

    static final Logger LOG = LoggerFactory.getLogger(RestOperation.class);

    final RestOperationParameters parameters;

    protected RestOperation(RestOperationParameters waitingRestOperationParameters) {
        this.parameters = waitingRestOperationParameters;
    }

    @SafeVarargs
    final ValidatableResponse validatedResponse(String url, String method, String body, String errorMessage, Predicate<ValidatableResponse>... predicates) {

        if (LOG.isInfoEnabled()) {
            LOG.info("cURL request: " + formatCurlConnectionInfo(parameters, url));
        }


        final RequestSpecification req = RestAssured.given().accept(ContentType.JSON);
        Optional.ofNullable(parameters.truststore()).ifPresent(ts -> req.trustStore(parameters.truststore()));
        parameters.addAuthorizationHeaders(req);
        ValidatableResponse response = switch (method.toUpperCase(Locale.ENGLISH)) {
            case HttpMethod.GET -> req.get(formatUrl(parameters, url)).then();
            case HttpMethod.PUT -> req.header(new Header("Content-Type", "application/json")).body(body).put(formatUrl(parameters, url)).then();
            case HttpMethod.POST -> req.header(new Header("Content-Type", "application/json")).body(body).post(formatUrl(parameters, url)).then();
            case HttpMethod.DELETE -> req.delete(formatUrl(parameters, url)).then();
            default -> throw Assertions.fail("Method not implemented");
        };
        final boolean responseOk = Arrays.stream(predicates).allMatch(p -> p.test(response));
        if (!responseOk) {
            LOG.error(serializeResponse(response.extract()));
            Assertions.fail(errorMessage);
        }

        return response;
    }

    String formatCurlConnectionInfo(RestOperationParameters port, String url) {
        List<String> builder = new LinkedList<>();
        builder.add("curl -k"); // trust self-signed certs
        builder.add(parameters.formatCurlAuthentication());
        builder.add(formatUrl(port, url));
        return String.join(" ", builder);
    }

    String serializeResponse(ExtractableResponse<Response> extract) {
        final String body = extract.body().asString();
        final int statusCode = extract.statusCode();
        final String statusLine = extract.statusLine();
        final String contentType = extract.contentType();
        final String headers = extract.headers().toString();
        return String.format(Locale.ROOT, "\nstatus code: %d\nstatus line: %s\n\nContent-type: %s\nheaders: %s\nbody: %s", statusCode, statusLine, contentType, headers, body);
    }

    String formatUrl(RestOperationParameters params, String url) {
        final boolean securedConnection = params.truststore() != null;
        String procotol = securedConnection ? "https" : "http";
        return procotol + "://localhost:" + params.port() + url;
    }
}
