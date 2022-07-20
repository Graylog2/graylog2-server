package org.graylog2.contentpacks.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.graylog2.contentpacks.facades.EntityWithExcerptFacade;
import org.graylog2.contentpacks.facades.UnsupportedEntityFacade;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.entities.EntityScope;
import org.graylog2.database.entities.ScopedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

/**
 * Injects specified _scope into content pack entity if appropriate.
 */
public class ScopedContentPackInjector {
    private static final Logger LOG = LoggerFactory.getLogger(ScopedContentPackInjector.class);
    private final ObjectMapper objectMapper;
    private final Map<ModelType, EntityWithExcerptFacade<?, ?>> entityFacades;

    @Inject
    public ScopedContentPackInjector(ObjectMapper objectMapper, Map<ModelType, EntityWithExcerptFacade<?, ?>> entityFacades) {
        this.objectMapper = objectMapper;
        this.entityFacades = entityFacades;
    }

    public void injectScope(Entity entity, EntityScope scope) {
        LOG.debug("");
        if (!(entity instanceof EntityV1)) {
            throw new IllegalArgumentException("Cannot inject scope into non-EntityV1 instances.");
        }
        // Use the facade to identify if the content pack entity type supports scoped entities.
        // It would have been nicer to use an entity condition similar to (entity instanceof ScopedContentPackEntity),
        // But, that is only possible by parsing the entity.data, and that is already done in the facade. So, I think
        // leaving it like this is best for now.
        final EntityWithExcerptFacade facade = entityFacades.getOrDefault(entity.type(), UnsupportedEntityFacade.INSTANCE);
        if (facade.usesScopedEntities()) {
            // TODO: Is there a better way to do this instead of doing the manual JSON manipulation?
            // This class needs a unit test, which should be easy to add.
            final JsonNode illuminateScope = objectMapper.valueToTree(ValueReference.of(scope.getName()));
            ((ObjectNode) (((EntityV1) entity).data())).put(ScopedEntity.FIELD_SCOPE, illuminateScope);
        }
    }
}
