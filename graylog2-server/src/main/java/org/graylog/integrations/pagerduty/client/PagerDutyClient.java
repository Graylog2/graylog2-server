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
package org.graylog.integrations.pagerduty.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.graylog.integrations.pagerduty.dto.PagerDutyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.io.IOException;
import java.util.Locale;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * The Pager Duty REST client implementation class compatible with events V2. For more information
 * about the event structure please see
 * <a href="https://v2.developer.pagerduty.com/v2/docs/send-an-event-events-api-v2">the api</a>.
 *
 * This class is heavily based on the work committed by Jochen, James, Dennis, Padma, and Edgar
 * <a href="https://github.com/graylog-labs/graylog-plugin-pagerduty/">here</a>.
 *
 * @author Jochen Schalanda
 * @author James Carr
 * @author Dennis Oelkers
 * @author Padma Liyanage
 * @author Edgar Molina
 */
public class PagerDutyClient {
    private static final Logger LOG = LoggerFactory.getLogger(PagerDutyClient.class);

    @VisibleForTesting
    static final String API_URL = "https://events.pagerduty.com/v2/enqueue";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Inject
    public PagerDutyClient(final OkHttpClient httpClient,
                           final ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * This method POSTs a message to PagerDuty's events enqueue API.
     *
     * @param payloadString JSON representation of a valid PagerDuty change event or alert event
     * @return PagerDutyResponse object
     * @throws TemporaryPagerDutyClientException when a retryable error is encountered
     * @throws PermanentPagerDutyClientException when a non-retriable error is encountered
     */
    public PagerDutyResponse enqueue(String payloadString)
            throws TemporaryPagerDutyClientException, PermanentPagerDutyClientException {
        final Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(MediaType.parse(APPLICATION_JSON), payloadString))
                .build();

        LOG.debug("Triggering event in PagerDuty with POST payload: {}", payloadString);

        try (final Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            LOG.debug("PagerDuty POST completed in {}ms [HTTP {}].  Response body: {}",
                    response.receivedResponseAtMillis() - response.sentRequestAtMillis(),
                    response.code(), responseBody);
            if (!response.isSuccessful()) {
                if (400 == response.code()) {
                    // HTTP 400 - Bad Request
                    throw new PermanentPagerDutyClientException(String.format(Locale.ROOT, "Invalid request sent to PagerDuty [%s]",
                            response.body().string()));
                } else if (429 == response.code()) {
                    throw new TemporaryPagerDutyClientException("Too many PagerDuty API calls at one time");
                } else {
                    throw new TemporaryPagerDutyClientException(String.format(Locale.ROOT,
                            "HTTP %d - PagerDuty server encountered an internal error", response.code()));
                }
            }
            return objectMapper.readValue(responseBody, PagerDutyResponse.class);
        } catch (IOException e) {
            LOG.error("Error sending PagerDuty notification event: " + e.getMessage());
            throw new TemporaryPagerDutyClientException("There was an error sending the notification event.", e);
        }
    }

    public static class PermanentPagerDutyClientException extends Exception {
        public PermanentPagerDutyClientException(String msg) {
            super(msg);
        }

        public PermanentPagerDutyClientException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    public static class TemporaryPagerDutyClientException extends Exception {
        public TemporaryPagerDutyClientException(String msg) {
            super(msg);
        }

        public TemporaryPagerDutyClientException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
