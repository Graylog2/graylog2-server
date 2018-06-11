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
package org.graylog2.contentpacks.facades;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.LookupTableEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.lookup.LookupDefaultMultiValue;
import org.graylog2.lookup.LookupDefaultSingleValue;
import org.graylog2.lookup.db.DBLookupTableService;
import org.graylog2.lookup.dto.LookupTableDto;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class LookupTableFacade implements EntityFacade<LookupTableDto> {
    public static final ModelType TYPE = ModelTypes.LOOKUP_TABLE;

    private final ObjectMapper objectMapper;
    private final DBLookupTableService lookupTableService;

    @Inject
    public LookupTableFacade(ObjectMapper objectMapper,
                             DBLookupTableService lookupTableService) {
        this.objectMapper = objectMapper;
        this.lookupTableService = lookupTableService;
    }

    @Override
    public EntityWithConstraints encode(LookupTableDto lookupTableDto) {
        final LookupTableEntity lookupTableEntity = LookupTableEntity.create(
                ValueReference.of(lookupTableDto.name()),
                ValueReference.of(lookupTableDto.title()),
                ValueReference.of(lookupTableDto.description()),
                ValueReference.of(lookupTableDto.cacheId()),
                ValueReference.of(lookupTableDto.dataAdapterId()),
                ValueReference.of(lookupTableDto.defaultSingleValue()),
                ValueReference.of(lookupTableDto.defaultSingleValueType()),
                ValueReference.of(lookupTableDto.defaultMultiValue()),
                ValueReference.of(lookupTableDto.defaultMultiValueType()));
        final JsonNode data = objectMapper.convertValue(lookupTableEntity, JsonNode.class);
        final EntityV1 entity = EntityV1.builder()
                .id(ModelId.of(lookupTableDto.id()))
                .type(ModelTypes.LOOKUP_TABLE)
                .data(data)
                .build();
        return EntityWithConstraints.create(entity);
    }

    @Override
    public LookupTableDto decode(Entity entity, Map<String, ValueReference> parameters, String username) {
        if (entity instanceof EntityV1) {
            return decodeEntityV1((EntityV1) entity, parameters);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private LookupTableDto decodeEntityV1(EntityV1 entity, Map<String, ValueReference> parameters) {
        final LookupTableEntity lookupTableEntity = objectMapper.convertValue(entity.data(), LookupTableEntity.class);
        final LookupTableDto lookupTableDto = LookupTableDto.builder()
                .name(lookupTableEntity.name().asString(parameters))
                .title(lookupTableEntity.title().asString(parameters))
                .description(lookupTableEntity.description().asString(parameters))
                .dataAdapterId(lookupTableEntity.dataAdapterName().asString(parameters))
                .cacheId(lookupTableEntity.cacheName().asString(parameters))
                .defaultSingleValue(lookupTableEntity.defaultSingleValue().asString(parameters))
                .defaultSingleValueType(lookupTableEntity.defaultSingleValueType().asEnum(parameters, LookupDefaultSingleValue.Type.class))
                .defaultMultiValue(lookupTableEntity.defaultMultiValue().asString(parameters))
                .defaultMultiValueType(lookupTableEntity.defaultMultiValueType().asEnum(parameters, LookupDefaultMultiValue.Type.class))
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

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return lookupTableService.findAll().stream()
                .map(this::createExcerpt)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<EntityWithConstraints> collectEntity(EntityDescriptor entityDescriptor) {
        final ModelId modelId = entityDescriptor.id();
        return lookupTableService.get(modelId.id()).map(this::encode);
    }

    @Override
    public Graph<EntityDescriptor> resolve(EntityDescriptor entityDescriptor) {
        final MutableGraph<EntityDescriptor> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entityDescriptor);

        final ModelId modelId = entityDescriptor.id();
        final Optional<LookupTableDto> lookupTableDto = lookupTableService.get(modelId.id());

        lookupTableDto.map(LookupTableDto::dataAdapterId)
                .map(dataAdapterId -> EntityDescriptor.create(ModelId.of(dataAdapterId), ModelTypes.LOOKUP_ADAPTER))
                .ifPresent(dataAdapter -> mutableGraph.putEdge(entityDescriptor, dataAdapter));
        lookupTableDto.map(LookupTableDto::cacheId)
                .map(cacheId -> EntityDescriptor.create(ModelId.of(cacheId), ModelTypes.LOOKUP_CACHE))
                .ifPresent(cache -> mutableGraph.putEdge(entityDescriptor, cache));

        return ImmutableGraph.copyOf(mutableGraph);
    }
}
