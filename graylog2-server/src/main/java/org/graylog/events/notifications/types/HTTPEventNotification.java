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
import com.google.common.collect.ImmutableList;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.graylog.events.configuration.EventsConfigurationProvider;
import org.graylog.events.notifications.EventNotification;
import org.graylog.events.notifications.EventNotificationContext;
import org.graylog.events.notifications.EventNotificationModelData;
import org.graylog.events.notifications.EventNotificationService;
import org.graylog.events.notifications.PermanentEventNotificationException;
import org.graylog.events.notifications.TemporaryEventNotificationException;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ParameterizedHttpClientProvider;
import org.graylog2.system.urlwhitelist.UrlWhitelistNotificationService;
import org.graylog2.system.urlwhitelist.UrlWhitelistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.io.IOException;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

public class HTTPEventNotification extends HTTPNotification implements EventNotification {
    public interface Factory extends EventNotification.Factory<HTTPEventNotification> {
        @Override
        HTTPEventNotification create();
    }

    private static final Logger LOG = LoggerFactory.getLogger(HTTPEventNotification.class);

    private static final MediaType CONTENT_TYPE = MediaType.parse(APPLICATION_JSON);

    private final EventNotificationService notificationCallbackService;
    private final ObjectMapper objectMapper;
    private final EventsConfigurationProvider configurationProvider;
    private final ParameterizedHttpClientProvider parameterizedHttpClientProvider;

    @Inject
    public HTTPEventNotification(EventNotificationService notificationCallbackService, ObjectMapper objectMapper,
                                 UrlWhitelistService whitelistService,
                                 UrlWhitelistNotificationService urlWhitelistNotificationService,
                                 EncryptedValueService encryptedValueService,
                                 EventsConfigurationProvider configurationProvider,
                                 final ParameterizedHttpClientProvider parameterizedHttpClientProvider) {
        super(whitelistService, urlWhitelistNotificationService, encryptedValueService);
        this.notificationCallbackService = notificationCallbackService;
        this.objectMapper = objectMapper;
        this.configurationProvider = configurationProvider;
        this.parameterizedHttpClientProvider = parameterizedHttpClientProvider;
    }

    /**
     * Depending on the configuration, either a default HTTP client will be returned or an instance
     * with {@link org.graylog2.shared.bindings.providers.TcpKeepAliveSocketFactory} configured.
     */
    private OkHttpClient selectClient(HTTPEventNotificationConfig notificationConfig) {
        final boolean withKeepAlive = configurationProvider.get().notificationsKeepAliveProbe();
        return parameterizedHttpClientProvider.get(withKeepAlive, notificationConfig.skipTLSVerification());
    }

    @Override
    public void execute(EventNotificationContext ctx) throws TemporaryEventNotificationException, PermanentEventNotificationException {
        final HTTPEventNotificationConfig config = (HTTPEventNotificationConfig) ctx.notificationConfig();
        ImmutableList<MessageSummary> backlog = notificationCallbackService.getBacklogForEvent(ctx);
        final EventNotificationModelData model = EventNotificationModelData.of(ctx, backlog);
        final HttpUrl httpUrl = validateUrl(config.url(), ctx.notificationId(), model.eventDefinitionTitle());


        final Request.Builder builder = new Request.Builder();
        addAuthHeader(builder, config.basicAuth());
        addApiKey(builder, httpUrl, config.apiKey(), config.apiSecret(), config.apiKeyAsHeader());

        final byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(model);
        } catch (JsonProcessingException e) {
            throw new PermanentEventNotificationException("Unable to serialize notification", e);
        }

        final Request request = builder
                .post(RequestBody.create(body, CONTENT_TYPE))
                .build();

        LOG.debug("Requesting HTTP endpoint at <{}> in notification <{}>",
                config.url(),
                ctx.notificationId());

        final OkHttpClient httpClient = selectClient(config);
        try (final Response r = httpClient.newCall(request).execute()) {
            if (!r.isSuccessful()) {
                throw new PermanentEventNotificationException(
                        "Expected successful HTTP response [2xx] but got [" + r.code() + "]. " + config.url());
            }
        } catch (IOException e) {
            throw new PermanentEventNotificationException(e.getMessage());
        }
    }
}
