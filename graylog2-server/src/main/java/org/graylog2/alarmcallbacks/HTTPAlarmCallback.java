/*
 * Copyright 2012-2014 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.alarmcallbacks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class HTTPAlarmCallback implements AlarmCallback {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    private Configuration configuration;
    private final AsyncHttpClient asyncHttpClient;
    private final ObjectMapper objectMapper;

    @Inject
    public HTTPAlarmCallback(AsyncHttpClient asyncHttpClient,
                             ObjectMapper objectMapper) {
        this.asyncHttpClient = asyncHttpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void initialize(Configuration config) throws AlarmCallbackConfigurationException {
        this.configuration = config;
    }

    @Override
    public void call(Stream stream, AlertCondition.CheckResult result) throws AlarmCallbackException {
        Map<String, Object> event = Maps.newHashMap();
        event.put("stream", stream);
        event.put("check_result", result);

        final String body;

        try {
            body = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            AlarmCallbackException exception = new AlarmCallbackException("Unable to serialize alarm");
            exception.initCause(e);
            throw exception;
        }

        final URL url;
        try {
             url = new URL(configuration.getString("url"));
        } catch (MalformedURLException e) {
            AlarmCallbackException exception = new AlarmCallbackException("Malformed URL");
            exception.initCause(e);
            throw exception;
        }

        Response r = null;
        try {
            r = asyncHttpClient.preparePost(url.toString())
                    .setBody(body)
                    .execute().get();
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new AlarmCallbackException(e.getMessage());
        }

        if (r.getStatusCode() != 200) {
            throw new AlarmCallbackException("Expected ping HTTP response [200] but got [" + r.getStatusCode() + "].");
        }
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        ConfigurationRequest configurationRequest = new ConfigurationRequest();
        configurationRequest.addField(new TextField("url",
                "URL",
                "https://example.org/alerts",
                "The URL to POST to when an alert is triggered",
                ConfigurationField.Optional.NOT_OPTIONAL));

        return configurationRequest;
    }

    @Override
    public String getName() {
        return "HTTP Alarm Callback";
    }

    @Override
    public Map<String, Object> getAttributes() {
        return configuration.getSource();
    }

    @Override
    public void checkConfiguration() throws ConfigurationException {
        if (configuration.getString("url") == null || configuration.getString("url").isEmpty())
            throw new ConfigurationException("URL parameter is missing!");
        try {
            URL url = new URL(configuration.getString("url"));
        } catch (MalformedURLException e) {
            ConfigurationException exception = new ConfigurationException("Malformed URL");
            exception.initCause(e);
            throw exception;
        }
    }
}
