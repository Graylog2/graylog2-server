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
package org.graylog.storage.opensearch2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.shaded.opensearch2.org.opensearch.action.support.master.AcknowledgedResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.ComposableIndexTemplateExistRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.DeleteComposableIndexTemplateRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.PutComposableIndexTemplateRequest;
import org.graylog.shaded.opensearch2.org.opensearch.cluster.metadata.ComposableIndexTemplate;
import org.graylog.shaded.opensearch2.org.opensearch.common.compress.CompressedXContent;
import org.graylog2.indexer.indices.Template;

import jakarta.inject.Inject;

import java.io.IOException;

public class ComposableIndexTemplateAdapter implements IndexTemplateAdapter {
    private final OpenSearchClient client;

    private final ObjectMapper objectMapper;

    @Inject
    public ComposableIndexTemplateAdapter(OpenSearchClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean ensureIndexTemplate(String templateName, Template template) {
        var serializedMapping = serialize(template.mappings());
        var settings = org.graylog.shaded.opensearch2.org.opensearch.common.settings.Settings.builder().loadFromMap(template.settings()).build();
        var osTemplate = new org.graylog.shaded.opensearch2.org.opensearch.cluster.metadata.Template(settings, serializedMapping, null);
        var indexTemplate = new ComposableIndexTemplate(template.indexPatterns(), osTemplate, null, template.order(), null, null);
        var request = new PutComposableIndexTemplateRequest()
                .name(templateName)
                .indexTemplate(indexTemplate);

        final AcknowledgedResponse result = client.execute((c, requestOptions) -> c.indices().putIndexTemplate(request, requestOptions),
                "Unable to create index template " + templateName);

        return result.isAcknowledged();
    }

    private CompressedXContent serialize(Object obj) {
        try {
            return new CompressedXContent(objectMapper.writeValueAsString(obj));
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
