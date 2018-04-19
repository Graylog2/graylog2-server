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
import org.graylog2.contentpacks.model.entities.LookupCacheEntity;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.lookup.db.DBCacheService;
import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.plugin.lookup.LookupCacheConfiguration;

import javax.inject.Inject;
import java.util.Map;

public class LookupCacheCodec implements EntityCodec<CacheDto> {
    private final ObjectMapper objectMapper;
    private final DBCacheService cacheService;

    @Inject
    public LookupCacheCodec(ObjectMapper objectMapper,
                            DBCacheService cacheService) {
        this.objectMapper = objectMapper;
        this.cacheService = cacheService;
    }

    @Override
    public Entity encode(CacheDto cacheDto) {
        // TODO: Create independent representation of entity?
        final Map<String, Object> configuration = objectMapper.convertValue(cacheDto.config(), TypeReferences.MAP_STRING_OBJECT);
        final LookupCacheEntity lookupCacheEntity = LookupCacheEntity.create(
                cacheDto.name(),
                cacheDto.title(),
                cacheDto.description(),
                configuration);
        final JsonNode data = objectMapper.convertValue(lookupCacheEntity, JsonNode.class);


        return EntityV1.builder()
                .id(ModelId.of(cacheDto.id()))
                .type(ModelTypes.LOOKUP_CACHE)
                .data(data)
                .build();
    }

    @Override
    public CacheDto decode(Entity entity) {
        if (entity instanceof EntityV1) {
            return decodeEntityV1((EntityV1) entity);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());

        }
    }

    private CacheDto decodeEntityV1(EntityV1 entity) {
        final LookupCacheEntity lookupCacheEntity = objectMapper.convertValue(entity.data(), LookupCacheEntity.class);
        final LookupCacheConfiguration configuration = objectMapper.convertValue(lookupCacheEntity.configuration(), LookupCacheConfiguration.class);
        final CacheDto cacheDto = CacheDto.builder()
                .name(lookupCacheEntity.name())
                .title(lookupCacheEntity.title())
                .description(lookupCacheEntity.description())
                .config(configuration)
                .build();

        return cacheService.save(cacheDto);
    }

    @Override
    public EntityExcerpt createExcerpt(CacheDto cacheDto) {
        return EntityExcerpt.builder()
                .id(ModelId.of(cacheDto.name()))
                .type(ModelTypes.LOOKUP_CACHE)
                .title(cacheDto.title())
                .build();

    }
}
