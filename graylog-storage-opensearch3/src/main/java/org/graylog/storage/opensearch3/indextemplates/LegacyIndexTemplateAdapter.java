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
package org.graylog.storage.opensearch3.indextemplates;

import jakarta.inject.Inject;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog2.indexer.indices.IndexTemplateAdapter;
import org.graylog2.indexer.indices.Template;
import org.opensearch.client.opensearch._types.AcknowledgedResponseBase;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.DeleteTemplateRequest;
import org.opensearch.client.opensearch.indices.ExistsTemplateRequest;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.opensearch.indices.PutTemplateRequest;
import org.opensearch.client.transport.endpoints.BooleanResponse;

public class LegacyIndexTemplateAdapter implements IndexTemplateAdapter {
    private final OfficialOpensearchClient client;
    private final OSSerializationUtils templateMapper;

    @Inject
    public LegacyIndexTemplateAdapter(final OfficialOpensearchClient client,
                                      final OSSerializationUtils templateMapper) {
        this.client = client;
        this.templateMapper = templateMapper;
    }

    @Override
    public boolean ensureIndexTemplate(final String templateName,
                                       final Template template) {
        try {
            final OpenSearchIndicesClient indicesClient = client.sync().indices();
            PutTemplateRequest putTemplateRequest = PutTemplateRequest.builder()
                    .name(templateName)
                    .indexPatterns(template.indexPatterns())
                    .mappings(templateMapper.fromMap(template.mappings(), TypeMapping._DESERIALIZER))
                    .settings(templateMapper.toJsonDataMap(template.settings()))
                    .order(template.order().intValue())
                    .build();
            final AcknowledgedResponseBase putTemplateResponse = indicesClient.putTemplate(putTemplateRequest);
            return putTemplateResponse.acknowledged();
        } catch (Throwable e) {
            throw OfficialOpensearchClient.mapException(e, "Failed to check if index template exists");
        }
    }

    @Override
    public boolean indexTemplateExists(final String templateName) {
        try {
            final OpenSearchIndicesClient indicesClient = client.sync().indices();
            final BooleanResponse booleanResponse = indicesClient.existsTemplate(ExistsTemplateRequest.builder().name(templateName).build());
            return booleanResponse.value();
        } catch (Throwable e) {
            throw OfficialOpensearchClient.mapException(e, "Failed to check if index template exists");
        }
    }

    @Override
    public boolean deleteIndexTemplate(final String templateName) {
        try {
            final OpenSearchIndicesClient indicesClient = client.sync().indices();
            final AcknowledgedResponseBase deleteTemplateResponse = indicesClient.deleteTemplate(DeleteTemplateRequest.builder().name(templateName).build());
            return deleteTemplateResponse.acknowledged();
        } catch (Throwable e) {
            throw OfficialOpensearchClient.mapException(e, "Failed to delete index template");
        }
    }
}
