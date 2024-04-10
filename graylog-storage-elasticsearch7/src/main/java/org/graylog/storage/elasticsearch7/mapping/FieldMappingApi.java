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
package org.graylog.storage.elasticsearch7.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Streams;
import jakarta.inject.Inject;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Request;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class FieldMappingApi {

    private final ElasticsearchClient client;

    @Inject
    public FieldMappingApi(ElasticsearchClient client) {
        this.client = client;
    }

    @AutoValue
    public static abstract class FieldMapping {
        public abstract String type();

        public abstract Optional<Boolean> fielddata();

        static FieldMapping create(String type, @Nullable Boolean fielddata) {
            return new AutoValue_FieldMappingApi_FieldMapping(type, Optional.ofNullable(fielddata));
        }
    }

    public Map<String, FieldMapping> fieldTypes(final String index) {
        final JsonNode result = client.executeRequest(request(index), "Unable to retrieve field types of index " + index);
        final JsonNode fields = result.path(index).path("mappings").path("properties");
        //noinspection UnstableApiUsage
        return Streams.stream(fields.fields())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    final JsonNode entryValue = entry.getValue();
                    String type = entryValue.path("type").asText();
                    if ("alias".equals(type)) {
                        String aliasPath = entryValue.path("path").asText();
                        type = fields.path(aliasPath).path("type").asText();
                    }
                    return FieldMapping.create(
                            type,
                            entryValue.path("fielddata").asBoolean()
                    );
                }));
    }

    private Request request(String index) {
        return new Request("GET", "/" + index + "/_mapping");
    }
}
