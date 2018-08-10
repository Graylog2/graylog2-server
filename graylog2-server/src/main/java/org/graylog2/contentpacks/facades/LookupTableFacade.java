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
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.exceptions.DivergingEntityConfigurationException;
import org.graylog2.contentpacks.exceptions.MissingNativeEntityException;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.LookupTableEntity;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.lookup.LookupDefaultMultiValue;
import org.graylog2.lookup.LookupDefaultSingleValue;
import org.graylog2.lookup.db.DBLookupTableService;
import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.lookup.dto.LookupTableDto;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class LookupTableFacade implements EntityFacade<LookupTableDto> {
    public static final ModelType TYPE = ModelTypes.LOOKUP_TABLE_V1;

    private final ObjectMapper objectMapper;
    private final DBLookupTableService lookupTableService;

    @Inject
    public LookupTableFacade(ObjectMapper objectMapper,
                             DBLookupTableService lookupTableService) {
        this.objectMapper = objectMapper;
        this.lookupTableService = lookupTableService;
    }

    @Override
    public EntityWithConstraints exportNativeEntity(LookupTableDto lookupTableDto) {
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
                .type(ModelTypes.LOOKUP_TABLE_V1)
                .data(data)
                .build();
        return EntityWithConstraints.create(entity);
    }

    @Override
    public NativeEntity<LookupTableDto> createNativeEntity(Entity entity,
                                                           Map<String, ValueReference> parameters,
                                                           Map<EntityDescriptor, Object> nativeEntities,
                                                           String username) {
        if (entity instanceof EntityV1) {
            return decode((EntityV1) entity, parameters, nativeEntities);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private NativeEntity<LookupTableDto> decode(EntityV1 entity,
                                                Map<String, ValueReference> parameters,
                                                Map<EntityDescriptor, Object> nativeEntities) {
        final LookupTableEntity lookupTableEntity = objectMapper.convertValue(entity.data(), LookupTableEntity.class);


        final String referencedDataAdapterName = lookupTableEntity.dataAdapterName().asString(parameters);
        final EntityDescriptor dataAdapterDescriptor = EntityDescriptor.create(referencedDataAdapterName, ModelTypes.LOOKUP_ADAPTER_V1);
        final Object dataAdapter = nativeEntities.get(dataAdapterDescriptor);
        final String dataAdapterId;
        if (dataAdapter instanceof DataAdapterDto) {
            dataAdapterId = ((DataAdapterDto) dataAdapter).id();
        } else {
            throw new MissingNativeEntityException(dataAdapterDescriptor);
        }

        final String referencedCacheName = lookupTableEntity.cacheName().asString(parameters);
        final EntityDescriptor cacheDescriptor = EntityDescriptor.create(referencedCacheName, ModelTypes.LOOKUP_CACHE_V1);
        final Object cache = nativeEntities.get(cacheDescriptor);
        final String cacheId;
        if (cache instanceof CacheDto) {
            cacheId = ((CacheDto) cache).id();
        } else {
            throw new MissingNativeEntityException(cacheDescriptor);
        }
        final LookupTableDto lookupTableDto = LookupTableDto.builder()
                .name(lookupTableEntity.name().asString(parameters))
                .title(lookupTableEntity.title().asString(parameters))
                .description(lookupTableEntity.description().asString(parameters))
                .dataAdapterId(dataAdapterId)
                .cacheId(cacheId)
                .defaultSingleValue(lookupTableEntity.defaultSingleValue().asString(parameters))
                .defaultSingleValueType(lookupTableEntity.defaultSingleValueType().asEnum(parameters, LookupDefaultSingleValue.Type.class))
                .defaultMultiValue(lookupTableEntity.defaultMultiValue().asString(parameters))
                .defaultMultiValueType(lookupTableEntity.defaultMultiValueType().asEnum(parameters, LookupDefaultMultiValue.Type.class))
                .build();
        final LookupTableDto savedLookupTableDto = lookupTableService.save(lookupTableDto);
        return NativeEntity.create(entity.id(), savedLookupTableDto.id(), TYPE, savedLookupTableDto);
    }

    @Override
    public Optional<NativeEntity<LookupTableDto>> findExisting(Entity entity, Map<String, ValueReference> parameters) {
        if (entity instanceof EntityV1) {
            return findExisting((EntityV1) entity, parameters);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private Optional<NativeEntity<LookupTableDto>> findExisting(EntityV1 entity, Map<String, ValueReference> parameters) {
        final LookupTableEntity lookupTableEntity = objectMapper.convertValue(entity.data(), LookupTableEntity.class);
        final String name = lookupTableEntity.name().asString(parameters);
        final String title = lookupTableEntity.title().asString(parameters);

        final Optional<LookupTableDto> lookupTable = lookupTableService.get(name);
        lookupTable.ifPresent(existingLookupTable -> compareLookupTable(name, title, existingLookupTable));

        return lookupTable.map(lt -> NativeEntity.create(entity.id(), lt.id(), TYPE, lt));
    }

    private void compareLookupTable(String name, String title, LookupTableDto existingLookupTable) {
        if (!name.equals(existingLookupTable.name()) || !title.equals(existingLookupTable.title())) {
            throw new DivergingEntityConfigurationException("Different lookup table configuration with name \"" + name + "\"");
        }
    }

    @Override
    public void delete(LookupTableDto nativeEntity) {
        lookupTableService.delete(nativeEntity.id());
    }

    @Override
    public EntityExcerpt createExcerpt(LookupTableDto lookupTableDto) {
        return EntityExcerpt.builder()
                .id(ModelId.of(lookupTableDto.name()))
                .type(ModelTypes.LOOKUP_TABLE_V1)
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
    public Optional<EntityWithConstraints> exportEntity(EntityDescriptor entityDescriptor) {
        final ModelId modelId = entityDescriptor.id();
        return lookupTableService.get(modelId.id()).map(this::exportNativeEntity);
    }

    @Override
    public Graph<EntityDescriptor> resolveNativeEntity(EntityDescriptor entityDescriptor) {
        final MutableGraph<EntityDescriptor> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entityDescriptor);

        final ModelId modelId = entityDescriptor.id();
        final Optional<LookupTableDto> lookupTableDto = lookupTableService.get(modelId.id());

        lookupTableDto.map(LookupTableDto::dataAdapterId)
                .map(dataAdapterId -> EntityDescriptor.create(dataAdapterId, ModelTypes.LOOKUP_ADAPTER_V1))
                .ifPresent(dataAdapter -> mutableGraph.putEdge(entityDescriptor, dataAdapter));
        lookupTableDto.map(LookupTableDto::cacheId)
                .map(cacheId -> EntityDescriptor.create(cacheId, ModelTypes.LOOKUP_CACHE_V1))
                .ifPresent(cache -> mutableGraph.putEdge(entityDescriptor, cache));

        return ImmutableGraph.copyOf(mutableGraph);
    }

    @Override
    public Graph<Entity> resolveForInstallation(Entity entity,
                                                Map<String, ValueReference> parameters,
                                                Map<EntityDescriptor, Entity> entities) {
        if (entity instanceof EntityV1) {
            return resolveForInstallation((EntityV1) entity, parameters, entities);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private Graph<Entity> resolveForInstallation(EntityV1 entity,
                                                 Map<String, ValueReference> parameters,
                                                 Map<EntityDescriptor, Entity> entities) {
        final MutableGraph<Entity> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entity);

        final LookupTableEntity lookupTableEntity = objectMapper.convertValue(entity.data(), LookupTableEntity.class);

        final String dataAdapterName = lookupTableEntity.dataAdapterName().asString(parameters);
        final EntityDescriptor dataAdapterDescriptor = EntityDescriptor.create(dataAdapterName, ModelTypes.LOOKUP_ADAPTER_V1);
        final Entity dataAdapterEntity = entities.get(dataAdapterDescriptor);
        if (dataAdapterEntity == null) {
            throw new ContentPackException("Missing data adapter \"" + dataAdapterName + "\" for lookup table " + entity.toEntityDescriptor());
        } else {
            mutableGraph.putEdge(entity, dataAdapterEntity);
        }

        final String cacheName = lookupTableEntity.cacheName().asString(parameters);
        final EntityDescriptor cacheDescriptor = EntityDescriptor.create(cacheName, ModelTypes.LOOKUP_CACHE_V1);
        final Entity cacheEntity = entities.get(cacheDescriptor);
        if (cacheEntity == null) {
            throw new ContentPackException("Missing cache \"" + cacheName + "\" for lookup table " + entity.toEntityDescriptor());
        } else {
            mutableGraph.putEdge(entity, cacheEntity);
        }

        return ImmutableGraph.copyOf(mutableGraph);
    }
}
