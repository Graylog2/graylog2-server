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
package org.graylog2.indexer.fieldtypes.utils;

import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FieldTypeDTOsMerger {

    public Collection<FieldTypeDTO> merge(final Collection<FieldTypeDTO> fromNewerIndex,
                                          final Collection<FieldTypeDTO> fromOlderIndex,
                                          final CustomFieldMappings customFieldMappings,
                                          final IndexFieldTypeProfile profile) {
        Map<String, FieldTypeDTO> result = new HashMap<>();
        if (fromNewerIndex != null) {
            fromNewerIndex.forEach(dto -> result.put(dto.fieldName(), dto));
        }
        if (fromOlderIndex != null) {
            fromOlderIndex.forEach(dto -> result.putIfAbsent(dto.fieldName(), dto));
        }
        if (profile != null) {
            profile.customFieldMappings().forEach(profileMapping ->
                    result.put(
                            profileMapping.fieldName(),
                            profileMapping.toFieldTypeDTO()
                    )
            );
        }
        if (customFieldMappings != null) {
            customFieldMappings.forEach(customFieldMapping ->
                    result.put(
                            customFieldMapping.fieldName(),
                            customFieldMapping.toFieldTypeDTO()
                    )
            );
        }

        return result.values();
    }
}
