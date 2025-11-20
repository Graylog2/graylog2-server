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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.graylog.storage.opensearch3.indextemplates.OSSerializationUtils;
import org.graylog2.indexer.indices.IndexTemplateAdapter;
import org.graylog2.indexer.indices.Template;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch.indices.DeleteIndexTemplateResponse;
import org.opensearch.client.opensearch.indices.PutIndexTemplateRequest;
import org.opensearch.client.opensearch.indices.PutIndexTemplateResponse;

import java.util.Map;
import java.util.stream.Collectors;

public class ComposableIndexTemplateAdapter implements IndexTemplateAdapter {
    private final OfficialOpensearchClient opensearchClient;
    private final OSSerializationUtils osSerializationUtils;

    @Inject
    public ComposableIndexTemplateAdapter(OfficialOpensearchClient opensearchClient, ObjectMapper objectMapper) {
        this.opensearchClient = opensearchClient;
        this.osSerializationUtils = new OSSerializationUtils();

    }

    @Override
    public boolean ensureIndexTemplate(String templateName, Template template) {


        PutIndexTemplateRequest request = new PutIndexTemplateRequest.Builder()
                .name(templateName)
                .indexPatterns(template.indexPatterns())
                .priority(Math.toIntExact(template.order()))
                .template(t -> t
                        .settings(s -> s.customSettings(toSettings(template.settings())))
                        .mappings(m -> m.properties(toProperties(template.mappings())))
                )
                .build();
        final PutIndexTemplateResponse result = opensearchClient.sync(c -> c.indices().putIndexTemplate(request), "Unable to create index template " + templateName);
        return result.acknowledged();
    }


    private Map<String, Property> toProperties(Template.Mappings mappings) {
        return mappings.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> osSerializationUtils.propertyOfType(String.valueOf(entry.getValue()))));
    }

    private Map<String, JsonData> toSettings(Template.Settings settings) {
        return settings.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> JsonData.of(entry.getValue())));
    }

    public boolean indexTemplateExists(String templateName) {
        return opensearchClient.sync(c -> c.indices().existsTemplate(r -> r.name(templateName)), "Unable to verify index template existence " + templateName).value();
    }

    public boolean deleteIndexTemplate(String templateName) {
        final DeleteIndexTemplateResponse result = opensearchClient.sync(c -> c.indices().deleteIndexTemplate(req -> req.name(templateName)), "Failed to delete compostable templates.");
        return result.acknowledged();
    }
}
