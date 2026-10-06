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
package org.graylog2.indexer.fieldtypes.mapping;

import jakarta.inject.Singleton;
import jakarta.ws.rs.BadRequestException;
import org.graylog2.indexer.indexset.CustomFieldMapping;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile;

import static org.graylog2.plugin.Message.FIELDS_UNCHANGEABLE_BY_CUSTOM_MAPPINGS;

@Singleton
public class CustomMappingValidation {

    public void checkType(final CustomFieldMapping customMapping) {
        var type = CustomFieldMappings.AVAILABLE_TYPES.get(customMapping.type());
        if (type == null) {
            throw new BadRequestException("Invalid type provided: " + customMapping.type() + " - available types: " + CustomFieldMappings.AVAILABLE_TYPES.keySet());
        }
    }

    public void checkFieldTypeCanBeChanged(final String fieldName) {
        if (FIELDS_UNCHANGEABLE_BY_CUSTOM_MAPPINGS.contains(fieldName)) {
            throw new BadRequestException("Unable to change field type of " + fieldName + ", not allowed to change type of these fields: " + FIELDS_UNCHANGEABLE_BY_CUSTOM_MAPPINGS);
        }
    }

    public void checkProfile(final IndexFieldTypeProfile profile) {
        profile.customFieldMappings().forEach(mapping -> {
            checkFieldTypeCanBeChanged(mapping.fieldName());
            checkType(mapping);
        });
    }

}
