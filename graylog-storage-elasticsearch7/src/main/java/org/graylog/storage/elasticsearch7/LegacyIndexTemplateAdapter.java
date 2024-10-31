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

import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.IndexTemplatesExistRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.graylog2.indexer.indices.Template;

import jakarta.inject.Inject;

import java.util.Map;

public class LegacyIndexTemplateAdapter implements IndexTemplateAdapter {
    private final ElasticsearchClient client;

    @Inject
    public LegacyIndexTemplateAdapter(ElasticsearchClient client) {
        this.client = client;
    }

    @Override
    public boolean ensureIndexTemplate(String templateName, Template template) {
        final Map<String, Object> templateSource = Map.of(
                "index_patterns", template.indexPatterns(),
                "mappings", template.mappings(),
                "settings", template.settings(),
                "order", template.order()
        );
        final PutIndexTemplateRequest request = new PutIndexTemplateRequest(templateName)
                .source(templateSource);

        final AcknowledgedResponse result = client.execute((c, requestOptions) -> c.indices().putTemplate(request, requestOptions),
                "Unable to create index template " + templateName);

        return result.isAcknowledged();
    }

    @Override
    public boolean indexTemplateExists(String templateName) {
        return client.execute((c, requestOptions) -> c.indices().existsTemplate(new IndexTemplatesExistRequest(templateName),
                requestOptions), "Unable to verify index template existence " + templateName);
    }

    @Override
    public boolean deleteIndexTemplate(String templateName) {
        final DeleteIndexTemplateRequest request = new DeleteIndexTemplateRequest(templateName);

        final AcknowledgedResponse result = client.execute((c, requestOptions) -> c.indices().deleteTemplate(request, requestOptions),
                "Unable to delete index template " + templateName);
        return result.isAcknowledged();
    }
}
