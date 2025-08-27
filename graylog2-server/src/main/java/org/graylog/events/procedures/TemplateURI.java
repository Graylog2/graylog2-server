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
package org.graylog.events.procedures;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

record TemplateURI(String path, Map<String, String> parameters) {
    private static final Logger LOG = LoggerFactory.getLogger(TemplateURI.class);

    static final String HTTP_EXTERNAL_URI = "${http_external_uri}";

    public String getLink() {
        final StringBuilder linkBuilder = new StringBuilder(HTTP_EXTERNAL_URI);
        linkBuilder.append(path);
        if (parameters != null && !parameters.isEmpty()) {
            final URIBuilder uriBuilder = new URIBuilder();
            parameters.forEach(uriBuilder::addParameter);
            try {
                linkBuilder.append("?").append(uriBuilder.build().getQuery());
            } catch (URISyntaxException e) {
                LOG.warn("Unable to build link: ", e);
            }
        }
        return linkBuilder.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String path;
        private Map<String, String> parameters;

        public Builder setPath(String rawPath) {
            this.path = rawPath;
            return this;
        }

        public Builder setParameters(Map<String, String> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder addParameter(String key, String value) {
            if (parameters == null) {
                parameters = new HashMap<>();
            }
            parameters.put(key, value);
            return this;
        }

        public Builder setTimeRange(String rawPath) {
            this.path = rawPath;
            return this;
        }

        TemplateURI build() {
            return new TemplateURI(path, parameters);
        }
    }
}
