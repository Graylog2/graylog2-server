package org.graylog2.entitygroups.contentpacks;

import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.facades.EntityFacade;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.entitygroups.model.EntityGroup;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

// TODO: Implement methods
public class EntityGroupFacade implements EntityFacade<EntityGroup> {
    public static final ModelType TYPE_V1 = ModelType.of("entity_group", "1");

    @Override
    public Optional<Entity> exportEntity(EntityDescriptor entityDescriptor, EntityDescriptorIds entityDescriptorIds) {
        return Optional.empty();
    }

    @Override
    public NativeEntity<EntityGroup> createNativeEntity(Entity entity, Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> nativeEntities, String username) throws InvalidRangeParametersException {
        return null;
    }

    @Override
    public Optional<NativeEntity<EntityGroup>> loadNativeEntity(NativeEntityDescriptor nativeEntityDescriptor) {
        return Optional.empty();
    }

    @Override
    public void delete(EntityGroup nativeEntity) {

    }

    @Override
    public EntityExcerpt createExcerpt(EntityGroup nativeEntity) {
        return null;
    }

    @Override
    public Set<EntityExcerpt> listEntityExcerpts() {
        return Set.of();
    }
}
