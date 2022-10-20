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
package org.graylog.events.notifications.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.graylog.events.notifications.EventNotification;
import org.graylog.events.notifications.EventNotificationContext;
import org.graylog.events.notifications.EventNotificationModelData;
import org.graylog.events.notifications.EventNotificationService;
import org.graylog.events.notifications.NotificationTestData;
import org.graylog.events.notifications.PermanentEventNotificationException;
import org.graylog.events.notifications.TemporaryEventNotificationException;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.OkHttpClientProvider;
import org.graylog2.system.urlwhitelist.UrlWhitelistNotificationService;
import org.graylog2.system.urlwhitelist.UrlWhitelistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class HTTPEventNotification implements EventNotification {
    public interface Factory extends EventNotification.Factory {
        @Override
        HTTPEventNotification create();
    }

    private static final Logger LOG = LoggerFactory.getLogger(HTTPEventNotification.class);

    private static final MediaType CONTENT_TYPE = MediaType.parse(APPLICATION_JSON);

    private final EventNotificationService notificationCallbackService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    private final UrlWhitelistService whitelistService;
    private final UrlWhitelistNotificationService urlWhitelistNotificationService;
    private final EncryptedValueService encryptedValueService;

    @Inject
    public HTTPEventNotification(EventNotificationService notificationCallbackService, ObjectMapper objectMapper,
                                 final OkHttpClientProvider httpClient, UrlWhitelistService whitelistService,
                                 UrlWhitelistNotificationService urlWhitelistNotificationService,
                                 EncryptedValueService encryptedValueService,
                                 @Named("http_enable_tcp_keepalive") boolean httpEnableTcpKeepAlive
                                 ) {
        this.notificationCallbackService = notificationCallbackService;
        this.objectMapper = objectMapper;
        this.httpClient = httpEnableTcpKeepAlive ? httpClient.getWithTcpKeepAlive() : httpClient.get();
        this.whitelistService = whitelistService;
        this.urlWhitelistNotificationService = urlWhitelistNotificationService;
        this.encryptedValueService = encryptedValueService;
    }

    @Override
    public void execute(EventNotificationContext ctx) throws TemporaryEventNotificationException, PermanentEventNotificationException {
        final HTTPEventNotificationConfig config = (HTTPEventNotificationConfig) ctx.notificationConfig();
        final HttpUrl httpUrl = HttpUrl.parse(config.url());

        if (httpUrl == null) {
            throw new TemporaryEventNotificationException(
                    "Malformed URL: <" + config.url() + "> in notification <" + ctx.notificationId() + ">");
        }

        ImmutableList<MessageSummary> backlog = notificationCallbackService.getBacklogForEvent(ctx);
        final EventNotificationModelData model = EventNotificationModelData.of(ctx, backlog);

        if (!whitelistService.isWhitelisted(config.url())) {
            if (!NotificationTestData.TEST_NOTIFICATION_ID.equals(ctx.notificationId())) {
                publishSystemNotificationForWhitelistFailure(config.url(), model.eventDefinitionTitle());
            }
            throw new TemporaryEventNotificationException("URL <" + config.url() + "> is not whitelisted.");
        }

        final byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(model);
        } catch (JsonProcessingException e) {
            throw new PermanentEventNotificationException("Unable to serialize notification", e);
        }

        final Request.Builder builder = new Request.Builder();
        final String basicAuthHeaderValue = getBasicAuthHeaderValue(config);
        if (!Strings.isNullOrEmpty(basicAuthHeaderValue)) {
            builder.addHeader("Authorization", basicAuthHeaderValue);
        }

        if (!Strings.isNullOrEmpty(config.apiKey())) {
            final String apiKeyValue = getApiKeyValue(config);
            HttpUrl urlWithApiKey = httpUrl.newBuilder().addQueryParameter(config.apiKey(), apiKeyValue).build();
            builder.url(urlWithApiKey);
        }
        else {
            builder.url(httpUrl);
        }

        final Request request = builder
                .post(RequestBody.create(CONTENT_TYPE, body))
                .build();

        LOG.debug("Requesting HTTP endpoint at <{}> in notification <{}>",
                config.url(),
                ctx.notificationId());

        try (final Response r = httpClient.newCall(request).execute()) {
            if (!r.isSuccessful()) {
                throw new PermanentEventNotificationException(
                        "Expected successful HTTP response [2xx] but got [" + r.code() + "]. " + config.url());
            }
        } catch (IOException e) {
            throw new PermanentEventNotificationException(e.getMessage());
        }
    }

    private void publishSystemNotificationForWhitelistFailure(String url, String eventNotificationTitle) {
        final String description = "The alert notification \"" + eventNotificationTitle +
                "\" is trying to access a URL which is not whitelisted. Please check your configuration. [url: \"" +
                url + "\"]";
        urlWhitelistNotificationService.publishWhitelistFailure(description);
    }

    private String getBasicAuthHeaderValue(HTTPEventNotificationConfig config) {
        if (config.basicAuth() == null || !config.basicAuth().isSet()) {
            return null;
        }
        String credentials = encryptedValueService.decrypt(config.basicAuth());
        return "Basic " + com.unboundid.util.Base64.encode(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private  String getApiKeyValue(HTTPEventNotificationConfig config) {
        if (config.apiSecret() == null || !config.apiSecret().isSet()) {
            return null;
        }
        return encryptedValueService.decrypt(config.apiSecret());
    }
}
