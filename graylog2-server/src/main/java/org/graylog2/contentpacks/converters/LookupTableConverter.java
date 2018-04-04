/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.contentpacks.converters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.lookup.dto.LookupTableDto;

import javax.inject.Inject;

public class LookupTableConverter implements EntityConverter<LookupTableDto> {
    private final ObjectMapper objectMapper;

    @Inject
    public LookupTableConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Entity convert(LookupTableDto lookupTableDto) {
        final JsonNode data = objectMapper.createObjectNode()
                .put("name", lookupTableDto.name())
                .put("title", lookupTableDto.title())
                .put("description", lookupTableDto.description())
                .put("adapter_id", lookupTableDto.dataAdapterId())
                .put("cache_id", lookupTableDto.cacheId())
                .put("default_single_value", lookupTableDto.defaultSingleValue())
                .put("default_single_value_type", lookupTableDto.defaultSingleValueType().name())
                .put("default_multi_value", lookupTableDto.defaultMultiValue())
                .put("default_multi_value_type", lookupTableDto.defaultMultiValueType().name());

        return EntityV1.builder()
                .id(ModelId.of(lookupTableDto.id()))
                .type(ModelTypes.LOOKUP_TABLE)
                .data(data)
                .build();
    }
}
