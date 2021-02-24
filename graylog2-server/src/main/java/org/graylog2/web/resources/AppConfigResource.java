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
package org.graylog2.web.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.floreysoft.jmte.Engine;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.graylog2.Configuration;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.rest.MoreMediaTypes;
import org.graylog2.rest.RestTools;
import org.graylog2.web.PluginUISettingsProvider;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Path("/config.js")
public class AppConfigResource {
    private final Configuration configuration;
    private final HttpConfiguration httpConfiguration;
    private final Engine templateEngine;
    private final Map<String, PluginUISettingsProvider> settingsProviders;
    private final ObjectMapper objectMapper;

    @Inject
    public AppConfigResource(Configuration configuration,
                             HttpConfiguration httpConfiguration,
                             Engine templateEngine,
                             Map<String, PluginUISettingsProvider> settingsProviders,
                             ObjectMapper objectMapper) {
        this.configuration = requireNonNull(configuration, "configuration");
        this.httpConfiguration = requireNonNull(httpConfiguration, "httpConfiguration");
        this.templateEngine = requireNonNull(templateEngine, "templateEngine");
        this.settingsProviders = requireNonNull(settingsProviders);
        this.objectMapper = objectMapper;
    }

    @GET
    @Produces(MoreMediaTypes.APPLICATION_JAVASCRIPT)
    public String get(@Context HttpHeaders headers) {
        final URL templateUrl = this.getClass().getResource("/web-interface/config.js.template");
        final String template;
        try {
            template = Resources.toString(templateUrl, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read AppConfig template while generating web interface configuration: ", e);
        }

        final URI baseUri = RestTools.buildExternalUri(headers.getRequestHeaders(), httpConfiguration.getHttpExternalUri());
        final Map<String, Object> model = ImmutableMap.of(
            "rootTimeZone", configuration.getRootTimeZone(),
            "serverUri", baseUri.resolve(HttpConfiguration.PATH_API),
            "appPathPrefix", baseUri.getPath(),
            "pluginUISettings", buildPluginUISettings());
        return templateEngine.transform(template, model);
    }

    private String buildPluginUISettings() {
        Map<String, Object> pluginUISettings = settingsProviders.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().pluginSettings()));
        try {
            return objectMapper.writeValueAsString(pluginUISettings);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
