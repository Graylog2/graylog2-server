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
package org.graylog.storage.opensearch3.mapping;

import jakarta.inject.Inject;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TextProperty;
import org.opensearch.client.opensearch.indices.GetMappingResponse;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class FieldMappingApi {
    private final OfficialOpensearchClient client;

    @Inject
    public FieldMappingApi(OfficialOpensearchClient client) {
        this.client = client;
    }

    public Map<String, FieldMapping> fieldTypes(final String index) {
        final GetMappingResponse response = client.sync(c -> c.indices().getMapping(fn -> fn.index(index)), "Unable to retrieve field types of index " + index);
        final Map<String, Property> properties = response.get(index).mappings().properties();
        return properties.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> toFieldMapping(e.getValue(), properties)));
    }

    private FieldMapping toFieldMapping(Property mapping, Map<String, Property> properties) {
        final Property.Kind kind = mapping._kind();
        if (kind == Property.Kind.Alias) {
            return resolveAlias(mapping, properties);
        } else {
            return new FieldMapping(kind.name().toLowerCase(Locale.ROOT), hasFieldData(mapping));
        }
    }

    private FieldMapping resolveAlias(Property mapping, Map<String, Property> properties) {
        final String path = mapping.alias().path();
        return toFieldMapping(properties.get(path), properties);
    }

    private boolean hasFieldData(Property value) {
        if (value._get() instanceof TextProperty textProperty) {
            return Optional.ofNullable(textProperty.fielddata()).orElse(false);
        }
        return false;
    }

    public record FieldMapping(String type, boolean fieldData) {
    }
}
