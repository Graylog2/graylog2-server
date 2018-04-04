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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.lookup.dto.CacheDto;

import javax.inject.Inject;

public class LookupCacheConverter implements EntityConverter<CacheDto> {
    private final ObjectMapper objectMapper;

    @Inject
    public LookupCacheConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Entity convert(CacheDto cacheDto) {
        final ObjectNode data = objectMapper.createObjectNode()
                .put("name", cacheDto.name())
                .put("title", cacheDto.title())
                .put("description", cacheDto.description());

        // TODO: Create independent representation of entity?
        data.set("config", objectMapper.convertValue(cacheDto.config(), JsonNode.class));

        return EntityV1.builder()
                .id(ModelId.of(cacheDto.id()))
                .type(ModelTypes.LOOKUP_CACHE)
                .data(data)
                .build();
    }
}
