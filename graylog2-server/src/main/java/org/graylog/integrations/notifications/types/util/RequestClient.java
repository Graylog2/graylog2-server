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
package org.graylog.integrations.notifications.types.util;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.graylog.events.notifications.PermanentEventNotificationException;
import org.graylog.events.notifications.TemporaryEventNotificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class RequestClient {
    private static final Logger LOG = LoggerFactory.getLogger(RequestClient.class);
    private final OkHttpClient httpClient;

    @Inject
    public RequestClient(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * @param message
     * @param webhookUrl
     * @throws TemporaryEventNotificationException - thrown for network or timeout type issues
     * @throws PermanentEventNotificationException - thrown with bad webhook url, authentication error type issues
     */
    public void send(String message, String webhookUrl) throws TemporaryEventNotificationException, PermanentEventNotificationException {

        final Request request = new Request.Builder()
                .url(webhookUrl)
                .post(RequestBody.create(MediaType.parse(APPLICATION_JSON), message))
                .build();

        LOG.debug("Posting to webhook url <{}> the payload is <{}>",
                webhookUrl,
                message);

        try (final Response r = httpClient.newCall(request).execute()) {
            if (!r.isSuccessful()) {
                throw new PermanentEventNotificationException(
                        "Expected successful HTTP response [2xx] but got [" + r.code() + "]. " + webhookUrl);
            }
        } catch (IOException e) {
            throw new TemporaryEventNotificationException("Unable to send the Message. " + e.getMessage());
        }
    }
}
