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

import javax.inject.Inject;
import java.util.Map;
import java.util.stream.Stream;

public class DashboardV1Facade extends ViewFacade {
    public static final ModelType TYPE_V1 = ModelTypes.DASHBOARD_V1;
    private ObjectMapper objectMapper;
    private EntityConverter entityConverter;

    @Inject
    public DashboardV1Facade(ObjectMapper objectMapper,
                             SearchDbService searchDbService,
                             EntityConverter entityConverter,
                             ViewService viewService) {
        super(objectMapper, searchDbService, viewService);
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
        return decode((EntityV1) entity, parameters, nativeEntities);
    }

    @Override
    protected NativeEntity<ViewDTO> decode(EntityV1 entityV1,
                                           Map<String, ValueReference> parameters,
                                           Map<EntityDescriptor, Object> nativeEntities) {
        final DashboardEntity dashboardEntity = objectMapper.convertValue(entityV1.data(), DashboardEntity.class);
        final ViewEntity viewEntity = entityConverter.convert(dashboardEntity, parameters);
        final JsonNode data = objectMapper.convertValue(viewEntity, JsonNode.class);
        final EntityV1 convertedEntity = entityV1.toBuilder().data(data).type(ModelTypes.DASHBOARD_V2).build();
        return super.decode(convertedEntity, parameters, nativeEntities);
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
