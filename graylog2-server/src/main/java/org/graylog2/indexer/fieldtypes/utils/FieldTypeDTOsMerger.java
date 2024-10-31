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
import org.graylog2.plugin.Message;
import org.graylog2.rest.resources.system.indexer.responses.FieldTypeOrigin;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetFieldType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.graylog2.indexer.fieldtypes.FieldTypeMapper.TYPE_MAP;
import static org.graylog2.indexer.indexset.CustomFieldMappings.REVERSE_TYPES;
import static org.graylog2.rest.resources.system.indexer.responses.FieldTypeOrigin.INDEX;
import static org.graylog2.rest.resources.system.indexer.responses.FieldTypeOrigin.OVERRIDDEN_INDEX;
import static org.graylog2.rest.resources.system.indexer.responses.FieldTypeOrigin.OVERRIDDEN_PROFILE;
import static org.graylog2.rest.resources.system.indexer.responses.FieldTypeOrigin.PROFILE;

public class FieldTypeDTOsMerger {

    public Collection<IndexSetFieldType> merge(final Collection<FieldTypeDTO> fromNewerIndex,
                                               final Collection<FieldTypeDTO> fromOlderIndex,
                                               final CustomFieldMappings customFieldMappings,
                                               final IndexFieldTypeProfile profile) {
        Map<String, IndexSetFieldType> result = new HashMap<>();
        if (fromNewerIndex != null) {
            fromNewerIndex.forEach(dto -> result.put(
                            dto.fieldName(),
                            toIndexSetFieldType(dto, INDEX)
                    )
            );
        }
        if (fromOlderIndex != null) {
            fromOlderIndex.forEach(dto -> result.putIfAbsent(
                            dto.fieldName(),
                            toIndexSetFieldType(dto, INDEX)
                    )
            );
        }
        if (profile != null) {
            profile.customFieldMappings().forEach(profileMapping ->
                    result.put(
                            profileMapping.fieldName(),
                            toIndexSetFieldType(profileMapping.toFieldTypeDTO(), PROFILE)
                    )
            );
        }
        if (customFieldMappings != null) {
            customFieldMappings.forEach(customFieldMapping -> {
                        final IndexSetFieldType indexSetFieldTypeFromPrevSources = result.get(customFieldMapping.fieldName());
                        result.put(
                                customFieldMapping.fieldName(),
                                toIndexSetFieldType(
                                        customFieldMapping.toFieldTypeDTO(),
                                        indexSetFieldTypeFromPrevSources != null && indexSetFieldTypeFromPrevSources.origin() == PROFILE
                                                ? OVERRIDDEN_PROFILE : OVERRIDDEN_INDEX
                                )

                        );
                    }
            );
        }

        return result.values();
    }

    private IndexSetFieldType toIndexSetFieldType(final FieldTypeDTO fieldTypeDTO, final FieldTypeOrigin origin) {
        return new IndexSetFieldType(
                fieldTypeDTO.fieldName(),
                REVERSE_TYPES.get(TYPE_MAP.get(fieldTypeDTO.physicalType())),
                origin,
                Message.FIELDS_UNCHANGEABLE_BY_CUSTOM_MAPPINGS.contains(fieldTypeDTO.fieldName()));
    }
}
