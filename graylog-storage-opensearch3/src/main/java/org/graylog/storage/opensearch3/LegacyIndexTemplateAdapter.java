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
package org.graylog.storage.opensearch3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.support.master.AcknowledgedResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.IndexTemplatesExistRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.PutIndexTemplateRequest;
import org.graylog2.indexer.indices.IndexTemplateAdapter;
import org.graylog2.indexer.indices.Template;

import jakarta.inject.Inject;
import org.opensearch.client.opensearch.generic.Body;
import org.opensearch.client.opensearch.generic.Request;
import org.opensearch.client.opensearch.generic.Response;

import java.io.IOException;
import java.util.Map;

public class LegacyIndexTemplateAdapter implements IndexTemplateAdapter {
    private final OfficialOpensearchClient opensearchClient;
    private final ObjectMapper objectMapper;

    @Inject
    public LegacyIndexTemplateAdapter(OfficialOpensearchClient opensearchClient, ObjectMapper objectMapper) {
        this.opensearchClient = opensearchClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean ensureIndexTemplate(String templateName, Template template) {
        var source = Map.of(
                "index_patterns", template.indexPatterns(),
                "mappings", template.mappings(),
                "settings", template.settings(),
                "order", template.order()
        );

        try {
            Request request = org.opensearch.client.opensearch.generic.Requests.builder()
                    .endpoint("/_template/" + templateName)
                    .method("PUT")
                    .body(Body.from(objectMapper.writeValueAsBytes(source), "application/json"))
                    .build();
            final boolean created = opensearchClient.sync(c -> {
                try (final Response res = c.generic().execute(request)) {
                    return res.getStatus() == 200;
                }
            }, "Failed to put template");

            if (!created) {
                throw new RuntimeException("Failed to put template " + templateName);
            }
            return true;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean indexTemplateExists(String templateName) {

        org.opensearch.client.opensearch.generic.Request newRequest = org.opensearch.client.opensearch.generic.Requests.builder()
                .endpoint("/_template/" + templateName)
                .method("HEAD")
                .build();
        return opensearchClient.sync(c -> {
            try (final org.opensearch.client.opensearch.generic.Response response = c.generic().execute(newRequest)) {
                return response.getStatus() == 200;
            }
        }, "Unable to verify index template existence " + templateName);
    }

    @Override
    public boolean deleteIndexTemplate(String templateName) {
        org.opensearch.client.opensearch.generic.Request request = org.opensearch.client.opensearch.generic.Requests.builder()
                .endpoint("/_template/" + templateName)
                .method("DELETE")
                .build();

        return opensearchClient.sync(c -> {
            try (final org.opensearch.client.opensearch.generic.Response response = c.generic().execute(request)) {
                return response.getStatus() == 200;
            }
        }, "Unable to delete index template " + templateName);
    }
}
