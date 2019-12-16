package org.graylog2.contentpacks.facades.dashboardV1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
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
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;

import java.util.Map;
import java.util.stream.Stream;

public class DashboardV1Facade extends ViewFacade {
    private ObjectMapper objectMapper;

    public DashboardV1Facade(ObjectMapper objectMapper, SearchDbService searchDbService, ViewService viewService) {
        super(objectMapper, searchDbService, viewService);
        this.objectMapper = objectMapper;
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
    public Stream<ViewDTO> getNativeViews() {
        /* There are no old dashboards in the system */
        return ImmutableSet.<ViewDTO>of().stream();
    }

    @Override
    public NativeEntity<ViewDTO> createNativeEntity(Entity entity, Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> nativeEntities, String username) {
        if (entity instanceof EntityV1) {
            try {
                return decode((EntityV1) entity, parameters, nativeEntities);
            } catch (InvalidRangeParametersException e) {
                throw new IllegalArgumentException("The provided entity does not have a valid TimeRange", e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    @Override
    protected NativeEntity<ViewDTO> decode(EntityV1 entityV1,
                                           Map<String, ValueReference> parameters,
                                           Map<EntityDescriptor, Object> nativeEntities) throws InvalidRangeParametersException {
        final DashboardEntity dashboardEntity = objectMapper.convertValue(entityV1.data(), DashboardEntity.class);
        final EntityConverter entityConverter = new EntityConverter(dashboardEntity, parameters);
        final ViewEntity viewEntity = entityConverter.convert();
        final JsonNode data = objectMapper.convertValue(viewEntity, JsonNode.class);
        final EntityV1 convertedEntity = entityV1.toBuilder().data(data).type(ModelTypes.DASHBOARD_V2).build();
        return super.decode(convertedEntity, parameters, nativeEntities);
    }
}
