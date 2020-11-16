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
package org.graylog2.alarmcallbacks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackConfigurationException;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.system.urlwhitelist.UrlWhitelistService;

import javax.inject.Inject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class HTTPAlarmCallback implements AlarmCallback {
    private static final String CK_URL = "url";
    private static final MediaType CONTENT_TYPE = MediaType.parse(APPLICATION_JSON);

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private Configuration configuration;
    private final UrlWhitelistService whitelistService;

    @Inject
    public HTTPAlarmCallback(final OkHttpClient httpClient, final ObjectMapper objectMapper,
            UrlWhitelistService whitelistService) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.whitelistService = whitelistService;
    }

    @Override
    public void initialize(final Configuration config) throws AlarmCallbackConfigurationException {
        this.configuration = config;
    }

    @Override
    public void call(final Stream stream, final AlertCondition.CheckResult result) throws AlarmCallbackException {
        final Map<String, Object> event = Maps.newHashMap();
        event.put("stream", stream);
        event.put("check_result", result);

        final byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(event);
        } catch (JsonProcessingException e) {
            throw new AlarmCallbackException("Unable to serialize alarm", e);
        }

        final String url = configuration.getString(CK_URL);
        final HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) {
            throw new AlarmCallbackException("Malformed URL: " + url);
        }

        if (!whitelistService.isWhitelisted(url)) {
            throw new AlarmCallbackException("URL <" + url + "> is not whitelisted.");
        }

        final Request request = new Request.Builder()
                .url(httpUrl)
                .post(RequestBody.create(CONTENT_TYPE, body))
                .build();
        try (final Response r = httpClient.newCall(request).execute()) {
            if (!r.isSuccessful()) {
                throw new AlarmCallbackException("Expected successful HTTP response [2xx] but got [" + r.code() + "].");
            }
        } catch (IOException e) {
            throw new AlarmCallbackException(e.getMessage(), e);
        }
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        final ConfigurationRequest configurationRequest = new ConfigurationRequest();
        configurationRequest.addField(new TextField(CK_URL,
                "URL",
                "https://example.org/alerts",
                "The URL to POST to when an alert is triggered",
                ConfigurationField.Optional.NOT_OPTIONAL));

        return configurationRequest;
    }

    @Override
    public String getName() {
        return "HTTP Alarm Callback [Deprecated]";
    }

    @Override
    public Map<String, Object> getAttributes() {
        return configuration.getSource();
    }

    @Override
    public void checkConfiguration() throws ConfigurationException {
        final String url = configuration.getString(CK_URL);
        if (isNullOrEmpty(url)) {
            throw new ConfigurationException("URL parameter is missing.");
        }

        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new ConfigurationException("Malformed URL '" + url + "'", e);
        }

        if (!whitelistService.isWhitelisted(url)) {
            throw new ConfigurationException("URL <" + url + "> is not whitelisted.");
        }
    }
}
