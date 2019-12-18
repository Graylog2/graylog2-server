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
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.plugins.views.search.views.ViewStateDTO;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.SearchEntity;
import org.graylog2.contentpacks.model.entities.ViewEntity;
import org.graylog2.contentpacks.model.entities.ViewStateEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ViewFacade implements EntityFacade<ViewDTO> {
    private static final Logger LOG = LoggerFactory.getLogger(ViewFacade.class);

    private final ObjectMapper objectMapper;
    private final ViewService viewService;
    private final SearchDbService searchDbService;

    @Inject
    public ViewFacade(ObjectMapper objectMapper,
                      SearchDbService searchDbService,
                      ViewService viewService) {
        this.objectMapper = objectMapper;
        this.searchDbService = searchDbService;
        this.viewService = viewService;
    }

    @Override
    public Optional<Entity> exportEntity(EntityDescriptor entityDescriptor,
                                         EntityDescriptorIds entityDescriptorIds) {
        final ModelId modelId = entityDescriptor.id();
        final Optional<ViewDTO> optionalView = viewService.get(modelId.id());
        if (optionalView.isPresent()) {
            return Optional.of(exportNativeEntity(optionalView.get(), entityDescriptorIds));
        }
        LOG.debug("Couldn't find view {}", entityDescriptor);
        return Optional.empty();
    }

    private Entity exportNativeEntity(ViewDTO view, EntityDescriptorIds entityDescriptorIds) {
        final ViewEntity.Builder viewEntityBuilder = view.toContentPackEntity(entityDescriptorIds);

        final Optional<Search> optionalSearch = searchDbService.get(view.searchId());
        final Search search = optionalSearch.orElseThrow(() ->
                new IllegalArgumentException("Search is missing in view " + view.searchId()));
        SearchEntity searchEntity = search.toContentPackEntity(entityDescriptorIds);
        final ViewEntity viewEntity = viewEntityBuilder.search(searchEntity).build();

        final JsonNode data = objectMapper.convertValue(viewEntity, JsonNode.class);
        return EntityV1.builder()
                .id(ModelId.of(entityDescriptorIds.getOrThrow(EntityDescriptor.create(view.id(), getModelType()))))
                .type(getModelType())
                .data(data)
                .build();
    }

    public abstract ModelType getModelType();

    @Override
    public NativeEntity<ViewDTO> createNativeEntity(Entity entity,
                                                    Map<String, ValueReference> parameters,
                                                    Map<EntityDescriptor, Object> nativeEntities,
                                                    String username) {
        if (entity instanceof EntityV1) {
            return decode((EntityV1) entity, parameters, nativeEntities);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private NativeEntity<ViewDTO> decode(EntityV1 entityV1,
                                         Map<String, ValueReference> parameters,
                                         Map<EntityDescriptor, Object> nativeEntities) {
        final ViewEntity viewEntity = objectMapper.convertValue(entityV1.data(), ViewEntity.class);
        final Map<String, ViewStateDTO> viewStateMap = new HashMap<>(viewEntity.state().size());
        for (Map.Entry<String, ViewStateEntity> entry : viewEntity.state().entrySet()) {
            final ViewStateEntity entity = entry.getValue();
            viewStateMap.put(entry.getKey(), entity.toNativeEntity(parameters, nativeEntities));
        }
        final ViewDTO.Builder viewBuilder = viewEntity.toNativeEntity(parameters, nativeEntities);
        viewBuilder.state(viewStateMap);
        final Search search = viewEntity.search().toNativeEntity(parameters, nativeEntities);
        final Search persistedSearch = searchDbService.save(search);

        final ViewDTO persistedView = viewService.save(viewBuilder.searchId(persistedSearch.id()).build());

        return NativeEntity.create(entityV1.id(), persistedView.id(), getModelType(), persistedView.title(), persistedView);
    }

    @Override
    public Optional<NativeEntity<ViewDTO>> loadNativeEntity(NativeEntityDescriptor nativeEntityDescriptor) {
        Optional<ViewDTO> optionalViewDTO = viewService.get(nativeEntityDescriptor.id().id());
        return optionalViewDTO.map(viewDTO -> NativeEntity.create(nativeEntityDescriptor, viewDTO));
    }

    @Override
    public void delete(ViewDTO nativeEntity) {
        viewService.delete(nativeEntity.id());
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return getNativeViews().map(this::createExcerpt).collect(Collectors.toSet());
    }

public Stream<ViewDTO> getNativeViews() {
        return viewService.streamAll().filter(v -> v.type().equals(this.getDTOType()));
    }

        @Override
    public EntityExcerpt createExcerpt(ViewDTO nativeEntity) {
        return EntityExcerpt.builder()
                .id(ModelId.of(nativeEntity.id()))
                .type(getModelType())
                .title(nativeEntity.title())
                .build();
    }

    public abstract ViewDTO.Type getDTOType();

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public Graph<EntityDescriptor> resolveNativeEntity(EntityDescriptor entityDescriptor) {
        final MutableGraph<EntityDescriptor> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entityDescriptor);

        final ModelId modelId = entityDescriptor.id();
        final ViewDTO viewDTO = viewService.get(modelId.id()).
                orElseThrow(() -> new NoSuchElementException("Could not find view with id " + modelId.id()));
        final Search search = searchDbService.get(viewDTO.searchId()).
                orElseThrow(() -> new NoSuchElementException("Could not find search with id " + viewDTO.searchId()));
        search.queries().stream().flatMap(q -> q.usedStreamIds().stream())
                .map(s -> EntityDescriptor.create(s, ModelTypes.STREAM_V1))
                .forEach(streamDescriptor -> mutableGraph.putEdge(entityDescriptor, streamDescriptor));
        return ImmutableGraph.copyOf(mutableGraph);
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public Graph<Entity> resolveForInstallation(Entity entity,
                                                Map<String, ValueReference> parameters,
                                                Map<EntityDescriptor, Entity> entities) {
        if (entity instanceof EntityV1) {
            return resolveEntityV1((EntityV1) entity, entities);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private Graph<Entity> resolveEntityV1(EntityV1 entity,
                                          Map<EntityDescriptor, Entity> entities) {
        final MutableGraph<Entity> mutableGraph = GraphBuilder.directed().build();
        mutableGraph.addNode(entity);

        final ViewEntity viewEntity = objectMapper.convertValue(entity.data(), ViewEntity.class);
        viewEntity.search().queries().stream().flatMap(q -> q.usedStreamIds().stream())
                .map(s -> EntityDescriptor.create(s, ModelTypes.STREAM_V1))
                .map(entities::get)
                .filter(Objects::nonNull)
                .forEach(stream -> mutableGraph.putEdge(entity, stream));
        return ImmutableGraph.copyOf(mutableGraph);
    }
}
