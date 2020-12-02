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
package org.graylog2.contentpacks.facades.dashboardV1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.Graph;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog2.contentpacks.facades.ViewFacade;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.DashboardEntity;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.ViewEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class DashboardV1Facade extends ViewFacade {
    public static final ModelType TYPE_V1 = ModelTypes.DASHBOARD_V1;
    private ObjectMapper objectMapper;
    private EntityConverter entityConverter;

    @Inject
    public DashboardV1Facade(ObjectMapper objectMapper,
                             SearchDbService searchDbService,
                             EntityConverter entityConverter,
                             ViewService viewService,
                             UserService userService) {
        super(objectMapper, searchDbService, viewService, userService);
        this.objectMapper = objectMapper;
        this.entityConverter = entityConverter;
    }

    @Override
    public ViewDTO.Type getDTOType() {
        return ViewDTO.Type.DASHBOARD;
    }

    @Override
    public ModelType getModelType() {
        return ModelTypes.DASHBOARD_V1;
    }

    @Override
    protected Stream<ViewDTO> getNativeViews() {
        /* There are no old dashboards in the system */
        return ImmutableSet.<ViewDTO>of().stream();
    }

    @Override
    public NativeEntity<ViewDTO> createNativeEntity(Entity entity, Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> nativeEntities, String username) {
        ensureV1(entity);
        final User user = Optional.ofNullable(userService.load(username)).orElseThrow(() -> new IllegalStateException("Cannot load user <" + username + "> from db"));
        return decode((EntityV1) entity, parameters, nativeEntities, user);
    }

    @Override
    protected NativeEntity<ViewDTO> decode(EntityV1 entityV1,
                                           Map<String, ValueReference> parameters,
                                           Map<EntityDescriptor, Object> nativeEntities, User user) {
        final EntityV1 convertedEntity = convertEntity(entityV1, parameters);
        return super.decode(convertedEntity, parameters, nativeEntities, user);
    }

    private EntityV1 convertEntity(EntityV1 entityV1,
                                   Map<String, ValueReference> parameters) {
        final DashboardEntity dashboardEntity = objectMapper.convertValue(entityV1.data(), DashboardEntity.class);
        final ViewEntity viewEntity = entityConverter.convert(dashboardEntity, parameters);
        final JsonNode data = objectMapper.convertValue(viewEntity, JsonNode.class);
        return entityV1.toBuilder().data(data).type(ModelTypes.DASHBOARD_V2).build();
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public Graph<Entity> resolveForInstallation(Entity entity,
                                                Map<String, ValueReference> parameters,
                                                Map<EntityDescriptor, Entity> entities) {
        ensureV1(entity);
        return resolveEntityV1((EntityV1) entity, parameters, entities);
    }

    @SuppressWarnings("UnstableApiUsage")
    private Graph<Entity> resolveEntityV1(EntityV1 entity,
                                          Map<String, ValueReference> parameters,
                                          Map<EntityDescriptor, Entity> entities) {

        final DashboardEntity dashboardEntity = objectMapper.convertValue(entity.data(), DashboardEntity.class);
        final ViewEntity viewEntity = entityConverter.convert(dashboardEntity, parameters);
        return resolveViewEntity(entity, viewEntity, entities);
    }
}
