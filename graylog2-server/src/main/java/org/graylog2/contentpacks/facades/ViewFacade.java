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
import org.graylog2.contentpacks.model.entities.ViewEntity;
import org.graylog2.contentpacks.model.entities.ViewStateEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ViewFacade implements EntityFacade<ViewDTO> {
    private static final Logger LOG = LoggerFactory.getLogger(ViewFacade.class);

    public static final ModelType TYPE_V1 = ModelTypes.VIEW_V1;

    private final ObjectMapper objectMapper;
    private final ViewService viewService;

    @Inject
    public ViewFacade(ObjectMapper objectMapper,
                      ViewService viewService) {
        this.objectMapper = objectMapper;
        this.viewService = viewService;
    }

    private Entity exportNativeEntity(ViewDTO view, EntityDescriptorIds entityDescriptorIds) {
        final Map<String, ViewStateEntity> viewStateMap = new HashMap<>(view.state().size());
        for (Map.Entry<String, ViewStateDTO> entry : view.state().entrySet()) {
           final ViewStateDTO viewStateDTO = entry.getValue();
           final ViewStateEntity.Builder viewStateBuilder = ViewStateEntity.builder()
                   .titles(viewStateDTO.titles())
                   .displayModeSettings(viewStateDTO.displayModeSettings())
                   .formatting(viewStateDTO.formatting())
                   .widgets(viewStateDTO.widgets())
                   .widgetPositions(viewStateDTO.widgetPositions())
                   .widgetMapping(viewStateDTO.widgetMapping());
           if (viewStateDTO.fields() != null && viewStateDTO.fields().isPresent()) {
               viewStateBuilder.fields(viewStateDTO.fields().get());
           }
           if (viewStateDTO.staticMessageListId() != null && viewStateDTO.staticMessageListId().isPresent()) {
               viewStateBuilder.staticMessageListId(viewStateDTO.staticMessageListId().get());
           }
           viewStateMap.put(entry.getKey(), viewStateBuilder.build());
        }
        final ViewEntity.Builder viewEntityBuilder = ViewEntity.builder()
                .type(view.type())
                .title(ValueReference.of(view.title()))
                .summary(ValueReference.of(view.summary()))
                .description(ValueReference.of(view.description()))
                .state(viewStateMap)
                .requires(view.requires())
                .properties(view.properties())
                .createdAt(view.createdAt());

        if (view.owner().isPresent()) {
            viewEntityBuilder.owner(view.owner().get());
        }

        final ViewEntity viewEntity = viewEntityBuilder.build();

        final JsonNode data = objectMapper.convertValue(viewEntity, JsonNode.class);
        return EntityV1.builder()
                .id(ModelId.of(entityDescriptorIds.getOrThrow(EntityDescriptor.create(view.id(), ModelTypes.VIEW_V1))))
                .type(ModelTypes.VIEW_V1)
                .data(data)
                .build();
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
            final ViewStateDTO.Builder viewStateBuilder = ViewStateDTO.builder()
                    .displayModeSettings(entity.displayModeSettings())
                    .widgets(entity.widgets())
                    .widgetMapping(entity.widgetMapping())
                    .widgetPositions(entity.widgetPositions())
                    .formatting(entity.formatting())
                    .titles(entity.titles());
            if (entity.fields() != null && entity.fields().isPresent()) {
                viewStateBuilder.fields(entity.fields().get());
            }
            if (entity.staticMessageListId() != null && entity.staticMessageListId().isPresent()) {
                viewStateBuilder.staticMessageListId(entity.staticMessageListId().get());
            }
            final ViewStateDTO viewStateDTO = viewStateBuilder.build();
            viewStateMap.put(entry.getKey(), viewStateDTO);
        }

        final ViewDTO.Builder viewBuilder = ViewDTO.builder()
                .title(viewEntity.title().asString(parameters))
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
        final ViewDTO peristedView = viewService.save(viewBuilder.build());
        return NativeEntity.create(entityV1.id(), peristedView.id(), TYPE_V1, peristedView.title(), peristedView);
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
                .type(TYPE_V1)
                .title(nativeEntity.title())
                .build();
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return viewService.streamAll().map(this::createExcerpt).collect(Collectors.toSet());
    }
}
