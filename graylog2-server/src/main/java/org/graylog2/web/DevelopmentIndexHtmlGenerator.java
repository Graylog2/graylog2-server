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
package org.graylog2.web;

import com.floreysoft.jmte.Engine;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.rest.RestTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Development implementation of the {@link IndexHtmlGenerator} interface that provides a dummy "index.html" page.
 *
 * This is used in the development where developers are running the web interface in an external process.
 */
@Singleton
public class DevelopmentIndexHtmlGenerator implements IndexHtmlGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(DevelopmentIndexHtmlGenerator.class);
    private static final String TEMPLATE_RESOURCE = "web-interface/index.html.development.template";

    private final Engine templateEngine;
    private final HttpConfiguration httpConfiguration;

    @Inject
    public DevelopmentIndexHtmlGenerator(final Engine templateEngine, final HttpConfiguration httpConfiguration) {
        this.templateEngine = templateEngine;
        this.httpConfiguration = httpConfiguration;
    }

    @Override
    public String get(MultivaluedMap<String, String> headers) {
        final Map<String, Object> model = ImmutableMap.<String, Object>builder()
                .put("title", "Graylog DEVELOPMENT Web Interface")
                .put("appPrefix", RestTools.buildExternalUri(headers, httpConfiguration.getHttpExternalUri()))
                .build();
        return templateEngine.transform(getTemplate(), model);
    }

    private String getTemplate() {
        try {
            return Resources.toString(Resources.getResource(TEMPLATE_RESOURCE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Couldn't load template resource <{}>", TEMPLATE_RESOURCE, e);
            return "MISSING TEMPLATE -- THIS IS A BUG!";
        }
    }
}
