/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
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
    private final UserService userService;

    @Inject
    public NotificationFacade(ObjectMapper objectMapper,
                              NotificationResourceHandler notificationResourceHandler,
                              DBNotificationService notificationService,
                              UserService userService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.notificationResourceHandler = notificationResourceHandler;
        this.userService = userService;
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
            final User user = Optional.ofNullable(userService.load(username)).orElseThrow(() -> new IllegalStateException("Cannot load user <" + username + "> from db"));
            return decode((EntityV1) entity, parameters, nativeEntities, user);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    private NativeEntity<NotificationDto> decode(EntityV1 entityV1,
                                                 Map<String, ValueReference> parameters,
                                                 Map<EntityDescriptor, Object> nativeEntities, User user) {
        final NotificationEntity entity = objectMapper.convertValue(entityV1.data(), NotificationEntity.class);
        final NotificationDto notificationDto = entity.toNativeEntity(parameters, nativeEntities);
        final NotificationDto savedDto = notificationResourceHandler.create(notificationDto, Optional.ofNullable(user));
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
