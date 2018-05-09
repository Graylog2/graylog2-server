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
package org.graylog2.contentpacks.codecs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.LookupTableEntity;
import org.graylog2.lookup.db.DBLookupTableService;
import org.graylog2.lookup.dto.LookupTableDto;

import javax.inject.Inject;

public class LookupTableCodec implements EntityCodec<LookupTableDto> {
    private final ObjectMapper objectMapper;
    private final DBLookupTableService lookupTableService;

    @Inject
    public LookupTableCodec(ObjectMapper objectMapper,
                            DBLookupTableService lookupTableService) {
        this.objectMapper = objectMapper;
        this.lookupTableService = lookupTableService;
    }

    @Override
    public EntityWithConstraints encode(LookupTableDto lookupTableDto) {
        final LookupTableEntity lookupTableEntity = LookupTableEntity.create(
                lookupTableDto.name(),
                lookupTableDto.title(),
                lookupTableDto.description(),
                lookupTableDto.cacheId(),
                lookupTableDto.dataAdapterId(),
                lookupTableDto.defaultSingleValue(),
                lookupTableDto.defaultSingleValueType(),
                lookupTableDto.defaultMultiValue(),
                lookupTableDto.defaultMultiValueType());
        final JsonNode data = objectMapper.convertValue(lookupTableEntity, JsonNode.class);
        final EntityV1 entity = EntityV1.builder()
                .id(ModelId.of(lookupTableDto.id()))
                .type(ModelTypes.LOOKUP_TABLE)
                .data(data)
                .build();
        return EntityWithConstraints.create(entity);
    }

    @Override
    public LookupTableDto decode(Entity entity) {
        if (entity instanceof EntityV1) {
            return decodeEntityV1((EntityV1) entity);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private LookupTableDto decodeEntityV1(EntityV1 entity) {
        final LookupTableEntity lookupTableEntity = objectMapper.convertValue(entity.data(), LookupTableEntity.class);
        final LookupTableDto lookupTableDto = LookupTableDto.builder()
                .name(lookupTableEntity.name())
                .title(lookupTableEntity.title())
                .description(lookupTableEntity.description())
                .dataAdapterId(lookupTableEntity.dataAdapterName())
                .cacheId(lookupTableEntity.cacheName())
                .defaultSingleValue(lookupTableEntity.defaultSingleValue())
                .defaultSingleValueType(lookupTableEntity.defaultSingleValueType())
                .defaultMultiValue(lookupTableEntity.defaultMultiValue())
                .defaultMultiValueType(lookupTableEntity.defaultMultiValueType())
                .build();
        return lookupTableService.save(lookupTableDto);
    }

    @Override
    public EntityExcerpt createExcerpt(LookupTableDto lookupTableDto) {
        return EntityExcerpt.builder()
                .id(ModelId.of(lookupTableDto.name()))
                .type(ModelTypes.LOOKUP_TABLE)
                .title(lookupTableDto.title())
                .build();
    }
}
