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
import org.apache.commons.lang.StringUtils;
import org.graylog.events.configuration.EventsConfigurationProvider;
import org.graylog.events.notifications.EventNotification;
import org.graylog.events.notifications.EventNotificationContext;
import org.graylog.events.notifications.EventNotificationModelData;
import org.graylog.events.notifications.EventNotificationService;
import org.graylog.events.notifications.PermanentEventNotificationException;
import org.graylog.events.notifications.TemporaryEventNotificationException;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.bindings.providers.ParameterizedHttpClientProvider;
import org.graylog2.system.urlwhitelist.UrlWhitelistNotificationService;
import org.graylog2.system.urlwhitelist.UrlWhitelistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.graylog.events.event.EventDto.FIELD_EVENT_TIMESTAMP;
import static org.graylog.events.event.EventDto.FIELD_FIELDS;
import static org.graylog.events.event.EventDto.FIELD_ID;
import static org.graylog.events.event.EventDto.FIELD_KEY;
import static org.graylog.events.event.EventDto.FIELD_KEY_TUPLE;
import static org.graylog.events.event.EventDto.FIELD_MESSAGE;
import static org.graylog.events.event.EventDto.FIELD_ORIGIN_CONTEXT;
import static org.graylog.events.event.EventDto.FIELD_PRIORITY;
import static org.graylog.events.event.EventDto.FIELD_PROCESSING_TIMESTAMP;
import static org.graylog.events.event.EventDto.FIELD_SOURCE;
import static org.graylog.events.event.EventDto.FIELD_SOURCE_STREAMS;
import static org.graylog.events.event.EventDto.FIELD_STREAMS;
import static org.graylog.events.event.EventDto.FIELD_TIMERANGE_END;
import static org.graylog.events.event.EventDto.FIELD_TIMERANGE_START;
import static org.graylog.events.notifications.EventNotificationModelData.FIELD_BACKLOG;
import static org.graylog.events.notifications.EventNotificationModelData.FIELD_EVENT_DEFINITION_DESCRIPTION;
import static org.graylog.events.notifications.EventNotificationModelData.FIELD_EVENT_DEFINITION_ID;
import static org.graylog.events.notifications.EventNotificationModelData.FIELD_EVENT_DEFINITION_TITLE;
import static org.graylog.events.notifications.EventNotificationModelData.FIELD_EVENT_DEFINITION_TYPE;
import static org.graylog.events.notifications.EventNotificationModelData.FIELD_JOB_DEFINITION_ID;
import static org.graylog.events.notifications.EventNotificationModelData.FIELD_JOB_TRIGGER_ID;

public class HTTPEventNotificationV2 extends HTTPNotification implements EventNotification {
    public interface Factory extends EventNotification.Factory<HTTPEventNotificationV2> {
        @Override
        HTTPEventNotificationV2 create();
    }

    private static final Logger LOG = LoggerFactory.getLogger(HTTPEventNotificationV2.class);
    private static final String EVENT = "event_";

    private final EventNotificationService notificationCallbackService;
    private final ObjectMapperProvider objectMapperProvider;
    private final EventsConfigurationProvider configurationProvider;
    private final ParameterizedHttpClientProvider parameterizedHttpClientProvider;
    private final Engine templateEngine;
    private final NotificationService notificationService;
    private final NodeId nodeId;

    @Inject
    public HTTPEventNotificationV2(EventNotificationService notificationCallbackService, ObjectMapperProvider objectMapperProvider,
                                   UrlWhitelistService whitelistService,
                                   UrlWhitelistNotificationService urlWhitelistNotificationService,
                                   EncryptedValueService encryptedValueService,
                                   EventsConfigurationProvider configurationProvider,
                                   Engine templateEngine,
                                   NotificationService notificationService,
                                   NodeId nodeId,
                                   final ParameterizedHttpClientProvider parameterizedHttpClientProvider) {
        super(whitelistService, urlWhitelistNotificationService, encryptedValueService);
        this.notificationCallbackService = notificationCallbackService;
        this.objectMapperProvider = objectMapperProvider;
        this.configurationProvider = configurationProvider;
        this.parameterizedHttpClientProvider = parameterizedHttpClientProvider;
        this.templateEngine = templateEngine;
        this.notificationService = notificationService;
        this.nodeId = nodeId;
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
        addApiKey(builder, httpUrl, config.apiKey(), config.apiSecret(), config.apiKeyAsHeader());
        addHeaders(builder, config.headers());

        final String body;
        final String errorMessage;
        try {
            body = buildRequestBody(modelData, config);
        } catch (JsonProcessingException processingErr) {
            errorMessage = "Unable to serialize notification";
            createSystemErrorNotification(errorMessage + "for notification [" + ctx.notificationId() + "]");
            throw new PermanentEventNotificationException(errorMessage, processingErr);
        } catch (UnsupportedEncodingException encodingErr) {
            errorMessage = "Unable to URL encode notification body";
            createSystemErrorNotification(errorMessage + "for notification [" + ctx.notificationId() + "]");
            throw new PermanentEventNotificationException(errorMessage, encodingErr);
        }

        switch (config.httpMethod()) {
            case GET -> builder.get();
            case PUT -> builder.put(RequestBody.create(body, getMediaType(config.contentType())));
            case POST -> builder.post(RequestBody.create(body, getMediaType(config.contentType())));
        }

        LOG.debug("Requesting HTTP endpoint at <{}> in notification <{}>", config.url(), ctx.notificationId());

        final OkHttpClient httpClient = selectClient(config);
        try (final Response r = httpClient.newCall(builder.build()).execute()) {
            if (!r.isSuccessful()) {
                errorMessage = "Expected successful HTTP response [2xx] but got [" + r.code() + "]. " + config.url();
                createSystemErrorNotification(errorMessage + "for notification [" + ctx.notificationId() + "]");
                throw new PermanentEventNotificationException(errorMessage);
            }
        } catch (IOException e) {
            throw new PermanentEventNotificationException(e.getMessage());
        }
    }

    private void createSystemErrorNotification(String message) {
        final Notification systemNotification = notificationService.buildNow()
                .addNode(nodeId.getNodeId())
                .addType(Notification.Type.GENERIC)
                .addSeverity(Notification.Severity.URGENT)
                .addDetail("title", "Custom HTTP Notification Failed")
                .addDetail("description", message);
        notificationService.publishIfFirst(systemNotification);
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
        final Map<String, Object> modelMap = objectMapper.convertValue(modelData, TypeReferences.MAP_STRING_OBJECT);
        if (!Strings.isNullOrEmpty(bodyTemplate)) {
            if (config.contentType().equals(HTTPEventNotificationConfigV2.ContentType.FORM_DATA)) {
                final String[] parts = bodyTemplate.split("&");
                body = Arrays.stream(parts)
                        .map(part -> {
                            final int equalsIndex = part.indexOf("=");
                            final String encodedKey = urlEncode(part.substring(0, equalsIndex));
                            final String encodedValue = equalsIndex < part.length() - 1 ?
                                    urlEncode(templateEngine.transform(part.substring(equalsIndex + 1), modelMap)) : "";
                            return encodedKey + "=" + encodedValue;
                        })
                        .collect(Collectors.joining("&"));
            } else {
                body = templateEngine.transform(bodyTemplate, modelMap);
            }
        } else {
            if (config.contentType().equals(HTTPEventNotificationConfigV2.ContentType.FORM_DATA)) {
                final Map<String, Object> eventMap = objectMapper.convertValue(modelData.event(), TypeReferences.MAP_STRING_OBJECT);
                body = getUrlEncodedEvent(modelMap, eventMap);
            } else {
                body = objectMapper.writeValueAsString(modelData);
            }
        }
        return body;
    }

    private String getUrlEncodedEvent(Map<String, Object> modelMap, Map<String, Object> eventMap) {
        return StringUtils.chop(urlEncodedKeyValue(FIELD_EVENT_DEFINITION_ID, modelMap.get(FIELD_EVENT_DEFINITION_ID)) +
                urlEncodedKeyValue(FIELD_EVENT_DEFINITION_TYPE, modelMap.get(FIELD_EVENT_DEFINITION_TYPE)) +
                urlEncodedKeyValue(FIELD_EVENT_DEFINITION_TITLE, modelMap.get(FIELD_EVENT_DEFINITION_TITLE)) +
                urlEncodedKeyValue(FIELD_EVENT_DEFINITION_DESCRIPTION, modelMap.get(FIELD_EVENT_DEFINITION_DESCRIPTION)) +
                urlEncodedKeyValue(FIELD_JOB_DEFINITION_ID, modelMap.get(FIELD_JOB_DEFINITION_ID)) +
                urlEncodedKeyValue(FIELD_JOB_TRIGGER_ID, modelMap.get(FIELD_JOB_TRIGGER_ID)) +
                urlEncodedKeyValue(EVENT + FIELD_ID, eventMap.get(FIELD_ID)) +
                urlEncodedKeyValue(EVENT + FIELD_ORIGIN_CONTEXT, eventMap.get(FIELD_ORIGIN_CONTEXT)) +
                urlEncodedKeyValue(EVENT + FIELD_EVENT_TIMESTAMP, eventMap.get(FIELD_EVENT_TIMESTAMP)) +
                urlEncodedKeyValue(EVENT + FIELD_PROCESSING_TIMESTAMP, eventMap.get(FIELD_PROCESSING_TIMESTAMP)) +
                urlEncodedKeyValue(EVENT + FIELD_TIMERANGE_START, eventMap.get(FIELD_TIMERANGE_START)) +
                urlEncodedKeyValue(EVENT + FIELD_TIMERANGE_END, eventMap.get(FIELD_TIMERANGE_END)) +
                urlEncodedKeyValue(EVENT + FIELD_STREAMS, eventMap.get(FIELD_STREAMS)) +
                urlEncodedKeyValue(EVENT + FIELD_SOURCE_STREAMS, eventMap.get(FIELD_SOURCE_STREAMS)) +
                urlEncodedKeyValue(EVENT + FIELD_MESSAGE, eventMap.get(FIELD_MESSAGE)) +
                urlEncodedKeyValue(EVENT + FIELD_SOURCE, eventMap.get(FIELD_SOURCE)) +
                urlEncodedKeyValue(EVENT + FIELD_KEY_TUPLE, eventMap.get(FIELD_KEY_TUPLE)) +
                urlEncodedKeyValue(EVENT + FIELD_KEY, eventMap.get(FIELD_KEY)) +
                urlEncodedKeyValue(EVENT + FIELD_PRIORITY, eventMap.get(FIELD_PRIORITY)) +
                urlEncodedKeyValue(EVENT + FIELD_FIELDS, eventMap.get(FIELD_FIELDS)) +
                urlEncodedKeyValue(FIELD_BACKLOG, modelMap.get(FIELD_BACKLOG))
        );
    }

    private String urlEncodedKeyValue(String key, Object value) {
        return urlEncode(key) + "=" + (value != null ? urlEncode(value.toString()) : "") + "&";
    }

    private String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
    }

}
