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
package org.graylog.events.contentpack.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.events.contentpack.entities.NotificationEntity;
import org.graylog.events.notifications.DBNotificationService;
import org.graylog.events.notifications.NotificationDto;
import org.graylog.events.notifications.NotificationResourceHandler;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.facades.EntityFacade;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class NotificationFacade implements EntityFacade<NotificationDto> {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationFacade.class);

    private final ObjectMapper objectMapper;
    private final DBNotificationService notificationService;
    private final NotificationResourceHandler notificationResourceHandler;

    @Inject
    public NotificationFacade(ObjectMapper objectMapper,
                              NotificationResourceHandler notificationResourceHandler,
                              DBNotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.notificationResourceHandler = notificationResourceHandler;
    }

    @Override
    public Optional<Entity> exportEntity(EntityDescriptor entityDescriptor, EntityDescriptorIds entityDescriptorIds) {
        final ModelId modelId = entityDescriptor.id();
        final Optional<NotificationDto> notificationDto = notificationService.get(modelId.id());
        if (!notificationDto.isPresent()) {
            LOG.debug("Couldn't find notification {}", entityDescriptor);
            return Optional.empty();
        }

        final NotificationEntity entity = (NotificationEntity) notificationDto.get().toContentPackEntity(entityDescriptorIds);
        final JsonNode data = objectMapper.convertValue(entity, JsonNode.class);
        return Optional.of(
                EntityV1.builder()
                        .id(ModelId.of(entityDescriptorIds.getOrThrow(notificationDto.get().id(), ModelTypes.NOTIFICATION_V1)))
                        .type(ModelTypes.NOTIFICATION_V1)
                        .data(data)
                        .build());
    }

    @Override
    public NativeEntity<NotificationDto> createNativeEntity(Entity entity, Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> nativeEntities, String username) {
        if (entity instanceof EntityV1) {
            return decode((EntityV1) entity, parameters, nativeEntities);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private NativeEntity<NotificationDto> decode(EntityV1 entityV1,
                                                 Map<String, ValueReference> parameters,
                                                 Map<EntityDescriptor, Object> nativeEntities) {
        final NotificationEntity entity = objectMapper.convertValue(entityV1.data(), NotificationEntity.class);
        final NotificationDto notificationDto = entity.toNativeEntity(parameters, nativeEntities);
        final NotificationDto savedDto = notificationResourceHandler.create(notificationDto);
        return NativeEntity.create(entityV1.id(), savedDto.id(), ModelTypes.NOTIFICATION_V1, savedDto.title(), savedDto);
    }

    @Override
    public Optional<NativeEntity<NotificationDto>> loadNativeEntity(NativeEntityDescriptor nativeEntityDescriptor) {
        final Optional<NotificationDto> notificationDto = notificationService.get(nativeEntityDescriptor.id().id());
        return notificationDto.map(notification -> NativeEntity.create(nativeEntityDescriptor, notification));
    }

    @Override
    public void delete(NotificationDto nativeEntity) {
        notificationResourceHandler.delete(nativeEntity.id());
    }

    @Override
    public EntityExcerpt createExcerpt(NotificationDto nativeEntity) {
        return EntityExcerpt.builder()
                .id(ModelId.of(nativeEntity.id()))
                .type(ModelTypes.NOTIFICATION_V1)
                .title(nativeEntity.title())
                .build();
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return notificationService.streamAll()
                .map(this::createExcerpt)
                .collect(Collectors.toSet());
    }
}
