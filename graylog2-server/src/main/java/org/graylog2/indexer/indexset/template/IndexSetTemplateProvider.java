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
package org.graylog2.indexer.indexset.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.graylog2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import static org.graylog2.shared.utilities.StringUtils.f;

@Singleton
public class IndexSetTemplateProvider implements Provider<List<IndexSetTemplate>> {
    private static final Logger LOG = LoggerFactory.getLogger(IndexSetTemplateProvider.class);
    private static final String CLOUD_TEMPLATE_RESOURCE = "cloud_templates.json";
    private static final String ON_PREM_TEMPLATE_RESOURCE = "on_prem_templates.json";
    protected final ObjectMapper objectMapper;
    private List<IndexSetTemplate> templates;

    @Inject
    IndexSetTemplateProvider(ObjectMapper objectMapper, Configuration configuration) {
        this.objectMapper = objectMapper;
        String resourceName = configuration.isCloud() ? CLOUD_TEMPLATE_RESOURCE : ON_PREM_TEMPLATE_RESOURCE;
        try {
            templates = objectMapper.readerForListOf(IndexSetTemplate.class).readValue(getResourceUrl(resourceName));
        } catch (IOException e) {
            LOG.error("Error reading index set templates from {}", resourceName, e);
            templates = Collections.emptyList();
        }
    }

    @Override
    public List<IndexSetTemplate> get() {
        return templates;
    }

    private URL getResourceUrl(String resourceName) {
        URL url = Resources.getResource(getClass(), resourceName);
        if (url == null) {
            throw new IllegalArgumentException(f("Unable to find resource: %s", resourceName));
        }
        return url;
    }
}
