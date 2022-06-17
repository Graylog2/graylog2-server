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
package org.graylog.integrations.notifications.types;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import javax.inject.Singleton;
import java.io.IOException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Singleton
public class SlackClient {

    private static final Logger LOG = LoggerFactory.getLogger(SlackClient.class);
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;


    @Inject
    public SlackClient(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient.newBuilder().followRedirects(false).build();
        this.objectMapper = objectMapper;
    }


    /**
     * @param message
     * @param webhookUrl
     * @throws TemporaryEventNotificationException - thrown for network or timeout type issues
     * @throws PermanentEventNotificationException - thrown with bad webhook url, authentication error type issues
     */
    public void send(SlackMessage message, String webhookUrl) throws TemporaryEventNotificationException,
            PermanentEventNotificationException, JsonProcessingException {

        final Request request = new Request.Builder()
                .url(webhookUrl)
                .post(RequestBody.create(MediaType.parse(APPLICATION_JSON), objectMapper.writeValueAsString(message)))
                .build();

        LOG.debug("Posting to webhook url <{}> the payload is <{}>",
                webhookUrl,
                "");

        try (final Response r = httpClient.newCall(request).execute()) {
            if (!r.isSuccessful()) {
                //ideally this should not happen and the user is expected to fill the
                //right configuration , while setting up a notification
                throw new PermanentEventNotificationException(
                        "Expected successful HTTP response [2xx] but got [" + r.code() + "]. " + webhookUrl);
            }
        } catch (IOException e) {
            throw new TemporaryEventNotificationException("Unable to send the slack Message. " + e.getMessage());
        }
    }


}
