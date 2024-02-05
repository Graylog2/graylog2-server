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
package org.graylog.storage.elasticsearch7;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.ComposableIndexTemplateExistRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.DeleteComposableIndexTemplateRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.PutComposableIndexTemplateRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.cluster.metadata.ComposableIndexTemplate;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.compress.CompressedXContent;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.settings.Settings;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.XContentType;
import org.graylog2.indexer.indices.Template;

import jakarta.inject.Inject;

import java.io.IOException;

public class ComposableIndexTemplateAdapter implements IndexTemplateAdapter {
    private final ElasticsearchClient client;
    private final ObjectMapper objectMapper;

    @Inject
    public ComposableIndexTemplateAdapter(ElasticsearchClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean ensureIndexTemplate(String templateName, Template template) {
        var serializedMapping = serialize(template.mappings());
        var settings = Settings.builder().loadFromSource(serializeJson(template.settings()), XContentType.JSON).build();
        var esTemplate = new org.graylog.shaded.elasticsearch7.org.elasticsearch.cluster.metadata.Template(settings, serializedMapping, null);
        var indexTemplate = new ComposableIndexTemplate(template.indexPatterns(), esTemplate, null, template.order(), null, null);
        var request = new PutComposableIndexTemplateRequest()
                .name(templateName)
                .indexTemplate(indexTemplate);

        final AcknowledgedResponse result = client.execute((c, requestOptions) -> c.indices().putIndexTemplate(request, requestOptions),
                "Unable to create index template " + templateName);

        return result.isAcknowledged();
    }

    private String serializeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private CompressedXContent serialize(Object obj) {
        try {
            return new CompressedXContent(serializeJson(obj));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean indexTemplateExists(String templateName) {
        return client.execute((c, requestOptions) -> c.indices().existsIndexTemplate(new ComposableIndexTemplateExistRequest(templateName),
                requestOptions), "Unable to verify index template existence " + templateName);
    }

    @Override
    public boolean deleteIndexTemplate(String templateName) {
        var request = new DeleteComposableIndexTemplateRequest(templateName);

        final AcknowledgedResponse result = client.execute((c, requestOptions) -> c.indices().deleteIndexTemplate(request, requestOptions),
                "Unable to delete index template " + templateName);
        return result.isAcknowledged();
    }
}
