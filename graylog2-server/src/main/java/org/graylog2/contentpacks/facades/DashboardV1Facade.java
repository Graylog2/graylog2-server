package org.graylog2.contentpacks.facades;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.ViewEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import java.util.Map;
import java.util.stream.Stream;

public class DashboardV1Facade extends ViewFacade {
    public DashboardV1Facade(ObjectMapper objectMapper, SearchDbService searchDbService, ViewService viewService) {
        super(objectMapper, searchDbService, viewService);
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
           return decode((EntityV1) entity, parameters);
        } else {
            throw new IllegalArgumentException("Unsupported entity version: " + entity.getClass());
        }
    }

    @Override
    protected NativeEntity<ViewDTO> decode(EntityV1 entityV1, Map<String, ValueReference> parameters) {
        final
        final ViewEntity viewEntity = ViewEntity.builder()

    }
}
