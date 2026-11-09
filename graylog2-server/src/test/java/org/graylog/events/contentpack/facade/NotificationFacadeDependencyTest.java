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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.graph.Graph;
import com.google.common.graph.MutableGraph;
import org.graylog.events.contentpack.entities.EventNotificationConfigEntity;
import org.graylog.events.contentpack.entities.NotificationEntity;
import org.graylog.events.notifications.DBNotificationService;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog.events.notifications.NotificationDto;
import org.graylog.events.notifications.NotificationResourceHandler;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationFacadeDependencyTest {
    private static final ModelType REFERENCED_TYPE = ModelType.of("referenced_thing", "1");
    private static final EntityDescriptor REFERENCED = EntityDescriptor.create("ref-1", REFERENCED_TYPE);

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final DBNotificationService notificationService = mock(DBNotificationService.class);
    private NotificationFacade facade;

    @BeforeEach
    void setUp() {
        objectMapper.registerSubtypes(ReferencingConfigEntity.class);
        facade = new NotificationFacade(objectMapper, mock(NotificationResourceHandler.class), notificationService,
                mock(UserService.class));
    }

    @Test
    void resolveNativeEntityDelegatesToConfig() {
        final EventNotificationConfig config = mock(EventNotificationConfig.class);
        doAnswer(invocation -> {
            final MutableGraph<EntityDescriptor> graph = invocation.getArgument(1);
            graph.putEdge(invocation.getArgument(0), REFERENCED);
            return null;
        }).when(config).resolveNativeEntity(any(), any());
        when(notificationService.get("n-1")).thenReturn(Optional.of(
                NotificationDto.builder().id("n-1").title("t").description("d").config(config).build()));
        final EntityDescriptor notification = EntityDescriptor.create("n-1", ModelTypes.NOTIFICATION_V1);

        final Graph<EntityDescriptor> graph = facade.resolveNativeEntity(notification);

        assertThat(graph.successors(notification)).containsExactly(REFERENCED);
    }

    @Test
    void resolveNativeEntityReturnsLoneNodeForMissingNotification() {
        when(notificationService.get("missing")).thenReturn(Optional.empty());
        final EntityDescriptor notification = EntityDescriptor.create("missing", ModelTypes.NOTIFICATION_V1);

        final Graph<EntityDescriptor> graph = facade.resolveNativeEntity(notification);

        assertThat(graph.nodes()).containsExactly(notification);
    }

    @Test
    void resolveForInstallationDelegatesToConfigEntity() {
        final Entity referenced = EntityV1.builder()
                .id(ModelId.of("ref-1"))
                .type(REFERENCED_TYPE)
                .data(objectMapper.createObjectNode())
                .build();
        final NotificationEntity notificationEntity = NotificationEntity.builder()
                .title(ValueReference.of("t"))
                .description(ValueReference.of("d"))
                .config(new ReferencingConfigEntity(ReferencingConfigEntity.TYPE, "ref-1"))
                .build();
        final EntityV1 entity = EntityV1.builder()
                .id(ModelId.of("n-1"))
                .type(ModelTypes.NOTIFICATION_V1)
                .data(objectMapper.convertValue(notificationEntity, JsonNode.class))
                .build();

        final Graph<Entity> graph = facade.resolveForInstallation(entity, Map.of(), Map.of(REFERENCED, referenced));

        assertThat(graph.successors(entity)).containsExactly(referenced);
    }

    @JsonTypeName(ReferencingConfigEntity.TYPE)
    record ReferencingConfigEntity(@JsonProperty("type") String type,
                                   @JsonProperty("ref_id") String refId) implements EventNotificationConfigEntity {
        static final String TYPE = "referencing-config";

        @Override
        public EventNotificationConfig toNativeEntity(Map<String, ValueReference> parameters,
                                                      Map<EntityDescriptor, Object> nativeEntities) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void resolveForInstallation(EntityV1 entity,
                                           Map<String, ValueReference> parameters,
                                           Map<EntityDescriptor, Entity> entities,
                                           MutableGraph<Entity> graph) {
            final Entity referenced = entities.get(EntityDescriptor.create(refId, REFERENCED_TYPE));
            if (referenced != null) {
                graph.putEdge(entity, referenced);
            }
        }
    }
}
