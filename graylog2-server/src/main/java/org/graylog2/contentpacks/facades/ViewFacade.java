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
import org.graylog.plugins.views.search.views.WidgetDTO;
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
import org.graylog2.contentpacks.model.entities.WidgetEntity;
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

    public abstract ModelType getModelType();

    private Entity exportNativeEntity(ViewDTO view, EntityDescriptorIds entityDescriptorIds) {
        final Map<String, ViewStateEntity> viewStateMap = new HashMap<>(view.state().size());
        for (Map.Entry<String, ViewStateDTO> entry : view.state().entrySet()) {
           final ViewStateDTO viewStateDTO = entry.getValue();
           final ViewStateEntity viewStateEntity = exportViewStateEntity(viewStateDTO);
           viewStateMap.put(entry.getKey(), viewStateEntity);
        }

        SearchEntity searchEntity = exportSearch(view.searchId()).orElseThrow(
                () -> new IllegalArgumentException("View has no valid search")
        );

        final ViewEntity.Builder viewEntityBuilder = ViewEntity.builder()
                .type(view.type())
                .title(ValueReference.of(view.title()))
                .summary(ValueReference.of(view.summary()))
                .description(ValueReference.of(view.description()))
                .state(viewStateMap)
                .search(searchEntity)
                .requires(view.requires())
                .properties(view.properties())
                .createdAt(view.createdAt());

        if (view.owner().isPresent()) {
            viewEntityBuilder.owner(view.owner().get());
        }

        final ViewEntity viewEntity = viewEntityBuilder.build();

        final JsonNode data = objectMapper.convertValue(viewEntity, JsonNode.class);
        return EntityV1.builder()
                .id(ModelId.of(entityDescriptorIds.getOrThrow(EntityDescriptor.create(view.id(), getModelType()))))
                .type(getModelType())
                .data(data)
                .build();
    }

    private Optional<SearchEntity> exportSearch(String searchId) {
        final Optional<Search> optionalSearch = searchDbService.get(searchId);
        if (!optionalSearch.isPresent()) {
            return Optional.empty();
        }
        final Search search = optionalSearch.get();
        final SearchEntity.Builder searchEntityBuilder = SearchEntity.builder()
                .queries(search.queries())
                .parameters(search.parameters())
                .requires(search.requires())
                .createdAt(search.createdAt());
        if (search.owner().isPresent()) {
            searchEntityBuilder.owner(search.owner().get());
        }
        return Optional.of(searchEntityBuilder.build());
    }

    private WidgetEntity exportWidgetEntity(WidgetDTO widgetDTO) {
        final WidgetEntity.Builder builder = WidgetEntity.builder()
                .id(widgetDTO.id())
                .config(widgetDTO.config())
                .filter(widgetDTO.filter())
                .streams(widgetDTO.streams())
                .type(widgetDTO.type());
        if (widgetDTO.query().isPresent()) {
            builder.query(widgetDTO.query().get());
        }
        if (widgetDTO.timerange().isPresent()) {
            builder.timerange(widgetDTO.timerange().get());
        }
        return builder.build();
    }

    private ViewStateEntity exportViewStateEntity(ViewStateDTO viewStateDTO) {
        final ViewStateEntity.Builder viewStateBuilder = ViewStateEntity.builder()
                .titles(viewStateDTO.titles())
                .displayModeSettings(viewStateDTO.displayModeSettings())
                .formatting(viewStateDTO.formatting())
                .widgets(viewStateDTO.widgets().stream().map(this::exportWidgetEntity).collect(Collectors.toSet()))
                .widgetPositions(viewStateDTO.widgetPositions())
                .widgetMapping(viewStateDTO.widgetMapping());
        if (viewStateDTO.fields() != null && viewStateDTO.fields().isPresent()) {
            viewStateBuilder.fields(viewStateDTO.fields().get());
        }
        if (viewStateDTO.staticMessageListId() != null && viewStateDTO.staticMessageListId().isPresent()) {
            viewStateBuilder.staticMessageListId(viewStateDTO.staticMessageListId().get());
        }
        return viewStateBuilder.build();
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

    @Override
    public NativeEntity<ViewDTO> createNativeEntity(Entity entity,
                                                    Map<String, ValueReference> parameters,
                                                    Map<EntityDescriptor, Object> nativeEntities,
                                                    String username) {
        if (entity instanceof EntityV1) {
            return decode((EntityV1) entity, parameters);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private NativeEntity<ViewDTO> decode(EntityV1 entityV1,
                                         Map<String, ValueReference> parameters) {
        final ViewEntity viewEntity = objectMapper.convertValue(entityV1.data(), ViewEntity.class);
        final Map<String, ViewStateDTO> viewStateMap = new HashMap<>(viewEntity.state().size());
        for (Map.Entry<String, ViewStateEntity> entry : viewEntity.state().entrySet()) {
            final ViewStateEntity entity = entry.getValue();
            final ViewStateDTO viewStateDTO = decodeViewStateEntity(entity);
            viewStateMap.put(entry.getKey(), viewStateDTO);
        }

        final Search search = decodeSearch(viewEntity.search());
        final ViewDTO.Builder viewBuilder = ViewDTO.builder()
                .title(viewEntity.title().asString(parameters))
                .searchId(search.id())
                .summary(viewEntity.summary().asString(parameters))
                .description(viewEntity.description().asString(parameters))
                .type(viewEntity.dtoType())
                .properties(viewEntity.properties())
                .createdAt(viewEntity.createdAt())
                .state(viewStateMap)
                .requires(viewEntity.requires());
        if (viewEntity.owner().isPresent()) {
            viewBuilder.owner(viewEntity.owner().get());
        }
        final ViewDTO persistedView = viewService.save(viewBuilder.build());
        return NativeEntity.create(entityV1.id(), persistedView.id(), getModelType(), persistedView.title(), persistedView);
    }

    private ViewStateDTO decodeViewStateEntity(ViewStateEntity viewStateEntity) {
        final ViewStateDTO.Builder viewStateBuilder = ViewStateDTO.builder()
                .displayModeSettings(viewStateEntity.displayModeSettings())
                .widgets(viewStateEntity.widgets().stream().map(this::decodeWidgetEntity).collect(Collectors.toSet()))
                .widgetMapping(viewStateEntity.widgetMapping())
                .widgetPositions(viewStateEntity.widgetPositions())
                .formatting(viewStateEntity.formatting())
                .titles(viewStateEntity.titles());
        if (viewStateEntity.fields() != null && viewStateEntity.fields().isPresent()) {
            viewStateBuilder.fields(viewStateEntity.fields().get());
        }
        if (viewStateEntity.staticMessageListId() != null && viewStateEntity.staticMessageListId().isPresent()) {
            viewStateBuilder.staticMessageListId(viewStateEntity.staticMessageListId().get());
        }
        return viewStateBuilder.build();
    }

    private WidgetDTO decodeWidgetEntity(WidgetEntity widgetEntity) {
        final WidgetDTO.Builder widgetBuilder = WidgetDTO.builder()
                .config(widgetEntity.config())
                .filter(widgetEntity.filter())
                .id(widgetEntity.id())
                .streams(widgetEntity.streams())
                .type(widgetEntity.type());
        if (widgetEntity.query().isPresent()) {
            widgetBuilder.query(widgetEntity.query().get());
        }
        if (widgetEntity.timerange().isPresent()) {
            widgetBuilder.timerange(widgetEntity.timerange().get());
        }
        return widgetBuilder.build();
    }

    private Search decodeSearch(SearchEntity entity) {
        final Search.Builder searchBuilder = Search.builder()
                .queries(entity.queries())
                .parameters(entity.parameters())
                .requires(entity.requires())
                .createdAt(entity.createdAt());
        if (entity.owner().isPresent()) {
            searchBuilder.owner(entity.owner().get());
        }
        return searchDbService.save(searchBuilder.build());
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
    public EntityExcerpt createExcerpt(ViewDTO nativeEntity) {
        return EntityExcerpt.builder()
                .id(ModelId.of(nativeEntity.id()))
                .type(getModelType())
                .title(nativeEntity.title())
                .build();
    }

    public abstract Stream<ViewDTO> getNativeViews();

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return getNativeViews().map(this::createExcerpt).collect(Collectors.toSet());
    }

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

    @Override
    public Graph<Entity> resolveForInstallation(Entity entity,
                                                Map<String, ValueReference> parameters,
                                                Map<EntityDescriptor, Entity> entities) {
        if (entity instanceof EntityV1) {
            return resolveEntityV1((EntityV1) entity, parameters, entities);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private Graph<Entity> resolveEntityV1(EntityV1 entity,
                                          Map<String, ValueReference> parameters,
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
