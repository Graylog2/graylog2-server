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

public class NotificationFacade implements EntityFacade<NotificationDto> {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationFacade.class);

    private final ObjectMapper objectMapper;
    private final DBNotificationService notificationService;

    @Inject
    public NotificationFacade(ObjectMapper objectMapper,
                              DBNotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @Override
    public Optional<Entity> exportEntity(EntityDescriptor entityDescriptor, EntityDescriptorIds entityDescriptorIds) {
        final ModelId modelId = entityDescriptor.id();
        final Optional<NotificationDto> notificationDto = notificationService.get(modelId.id());
        if (!notificationDto.isPresent()) {
            LOG.debug("Couldn't find notification {}", entityDescriptor);
            return Optional.empty();
        }

        final NotificationEntity entity = (NotificationEntity) notificationDto.get().toContentPackEntity();
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
        return null;
    }

    @Override
    public Optional<NativeEntity<NotificationDto>> loadNativeEntity(NativeEntityDescriptor nativeEntityDescriptor) {
        return Optional.empty();
    }

    @Override
    public void delete(NotificationDto nativeEntity) {

    }

    @Override
    public EntityExcerpt createExcerpt(NotificationDto nativeEntity) {
        return null;
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return null;
    }
}
