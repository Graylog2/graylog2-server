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
import com.floreysoft.jmte.Engine;
import com.google.common.base.Strings;
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
import org.graylog2.jackson.TypeReferences;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.bindings.providers.ParameterizedHttpClientProvider;
import org.graylog2.system.urlwhitelist.UrlWhitelistNotificationService;
import org.graylog2.system.urlwhitelist.UrlWhitelistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

public class HTTPEventNotificationV2 extends HTTPNotification implements EventNotification {
    public interface Factory extends EventNotification.Factory<HTTPEventNotificationV2> {
        @Override
        HTTPEventNotificationV2 create();
    }

    private static final Logger LOG = LoggerFactory.getLogger(HTTPEventNotificationV2.class);

    private final EventNotificationService notificationCallbackService;
    private final ObjectMapperProvider objectMapperProvider;
    private final EventsConfigurationProvider configurationProvider;
    private final ParameterizedHttpClientProvider parameterizedHttpClientProvider;
    private final Engine templateEngine;

    @Inject
    public HTTPEventNotificationV2(EventNotificationService notificationCallbackService, ObjectMapperProvider objectMapperProvider,
                                   UrlWhitelistService whitelistService,
                                   UrlWhitelistNotificationService urlWhitelistNotificationService,
                                   EncryptedValueService encryptedValueService,
                                   EventsConfigurationProvider configurationProvider,
                                   Engine templateEngine,
                                   final ParameterizedHttpClientProvider parameterizedHttpClientProvider) {
        super(whitelistService, urlWhitelistNotificationService, encryptedValueService);
        this.notificationCallbackService = notificationCallbackService;
        this.objectMapperProvider = objectMapperProvider;
        this.configurationProvider = configurationProvider;
        this.parameterizedHttpClientProvider = parameterizedHttpClientProvider;
        this.templateEngine = templateEngine;
    }

    /**
     * Depending on the configuration, either a default HTTP client will be returned or an instance
     * with {@link org.graylog2.shared.bindings.providers.TcpKeepAliveSocketFactory} configured.
     */
    private OkHttpClient selectClient(HTTPEventNotificationConfigV2 notificationConfig) {
        final boolean withKeepAlive = configurationProvider.get().notificationsKeepAliveProbe();
        return parameterizedHttpClientProvider.get(withKeepAlive, notificationConfig.skipTLSVerification());
    }

    @Override
    public void execute(EventNotificationContext ctx) throws TemporaryEventNotificationException, PermanentEventNotificationException {
        final HTTPEventNotificationConfigV2 config = (HTTPEventNotificationConfigV2) ctx.notificationConfig();
        ImmutableList<MessageSummary> backlog = notificationCallbackService.getBacklogForEvent(ctx);
        final EventNotificationModelData modelData = EventNotificationModelData.of(ctx, backlog);
        final HttpUrl httpUrl = validateUrl(config.url(), ctx.notificationId(), modelData.eventDefinitionTitle());

        final Request.Builder builder = new Request.Builder();
        addAuthHeader(builder, config.basicAuth());
        addApiKey(builder, httpUrl, config.apiKey(), config.apiSecret());

        final String body;
        try {
            body = buildRequestBody(modelData, config);
        } catch (JsonProcessingException processingErr) {
            throw new PermanentEventNotificationException("Unable to serialize notification", processingErr);
        } catch (UnsupportedEncodingException encodingErr) {
            throw new PermanentEventNotificationException("Unable to URL encode notification body", encodingErr);
        }

        switch (config.httpMethod()) {
            case GET -> builder.get();
            case PUT -> builder.put(RequestBody.create(body, getMediaType(config.contentType())));
            case POST -> builder.post(RequestBody.create(body, getMediaType(config.contentType())));
        }

        LOG.debug("Requesting HTTP endpoint at <{}> in notification <{}>", config.url(), ctx.notificationId());

        final OkHttpClient httpClient = selectClient(config);
        try (final Response r = httpClient.newCall(builder .build()).execute()) {
            if (!r.isSuccessful()) {
                throw new PermanentEventNotificationException(
                        "Expected successful HTTP response [2xx] but got [" + r.code() + "]. " + config.url());
            }
        } catch (IOException e) {
            throw new PermanentEventNotificationException(e.getMessage());
        }
    }

    private MediaType getMediaType(HTTPEventNotificationConfigV2.ContentType contentType) {
        switch (contentType) {
            case FORM_DATA -> {
                return MediaType.parse(APPLICATION_FORM_URLENCODED);
            }
            case JSON -> {
                return MediaType.parse(APPLICATION_JSON);
            }
            case PLAIN_TEXT -> {
                return MediaType.parse(TEXT_PLAIN);
            }
            default -> {
                return null;
            }
        }
    }

    private String buildRequestBody(EventNotificationModelData modelData, HTTPEventNotificationConfigV2 config) throws JsonProcessingException, UnsupportedEncodingException {
        // If httpMethod is POST or PUT then contentType must be set for a valid config, but the second check removes
        // linter warning of a potential null pointer
        if (config.httpMethod().equals(HTTPEventNotificationConfigV2.HttpMethod.GET) || config.contentType() == null) {
            return "";
        }
        final String body;
        final String bodyTemplate = config.bodyTemplate();
        final ObjectMapper objectMapper = objectMapperProvider.getForTimeZone(config.timeZone());
        if (!Strings.isNullOrEmpty(bodyTemplate)) {
            final Map<String, Object> modelMap = objectMapper.convertValue(modelData, TypeReferences.MAP_STRING_OBJECT);
            body = templateEngine.transform(bodyTemplate, modelMap);
        } else {
            body = objectMapper.writeValueAsString(modelData);
        }
        return config.contentType().equals(HTTPEventNotificationConfigV2.ContentType.FORM_DATA) ?
                URLEncoder.encode(body, StandardCharsets.UTF_8).replaceAll("\\+", "%20") : body;
    }

}
