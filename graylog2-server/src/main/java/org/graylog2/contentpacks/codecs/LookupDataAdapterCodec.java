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
import org.graylog2.contentpacks.model.entities.LookupDataAdapterEntity;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.lookup.db.DBDataAdapterService;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;

import javax.inject.Inject;
import java.util.Map;

public class LookupDataAdapterCodec implements EntityCodec<DataAdapterDto> {
    private final ObjectMapper objectMapper;
    private final DBDataAdapterService dataAdapterService;

    @Inject
    public LookupDataAdapterCodec(ObjectMapper objectMapper,
                                  DBDataAdapterService dataAdapterService) {
        this.objectMapper = objectMapper;
        this.dataAdapterService = dataAdapterService;
    }

    @Override
    public Entity encode(DataAdapterDto dataAdapterDto) {
        // TODO: Create independent representation of entity?
        final Map<String, Object> configuration = objectMapper.convertValue(dataAdapterDto.config(), TypeReferences.MAP_STRING_OBJECT);
        final LookupDataAdapterEntity lookupDataAdapterEntity = LookupDataAdapterEntity.create(
                dataAdapterDto.name(),
                dataAdapterDto.title(),
                dataAdapterDto.description(),
                configuration);
        final JsonNode data = objectMapper.convertValue(lookupDataAdapterEntity, JsonNode.class);

        return EntityV1.builder()
                .id(ModelId.of(dataAdapterDto.id()))
                .type(ModelTypes.LOOKUP_ADAPTER)
                .data(data)
                .build();
    }

    @Override
    public DataAdapterDto decode(Entity entity) {
        if (entity instanceof EntityV1) {
            return decodeEntityV1((EntityV1) entity);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());

        }
    }

    private DataAdapterDto decodeEntityV1(EntityV1 entity) {
        final LookupDataAdapterEntity lookupDataAdapterEntity = objectMapper.convertValue(entity.data(), LookupDataAdapterEntity.class);
        final LookupDataAdapterConfiguration configuration = objectMapper.convertValue(lookupDataAdapterEntity.configuration(), LookupDataAdapterConfiguration.class);
        final DataAdapterDto dataAdapterDto = DataAdapterDto.builder()
                .name(lookupDataAdapterEntity.name())
                .title(lookupDataAdapterEntity.title())
                .description(lookupDataAdapterEntity.description())
                .config(configuration)
                .build();

        return dataAdapterService.save(dataAdapterDto);
    }

    @Override
    public EntityExcerpt createExcerpt(DataAdapterDto dataAdapterDto) {
        return EntityExcerpt.builder()
                .id(ModelId.of(dataAdapterDto.name()))
                .type(ModelTypes.LOOKUP_ADAPTER)
                .title(dataAdapterDto.title())
                .build();
    }
}
