package org.graylog2.contentpacks.facades;

import com.google.common.collect.ImmutableSet;
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.database.entities.EntityScope;
import org.graylog2.database.entities.EntityScopeService;

import java.util.Set;

public class EntityScopeTestUtil {

    public static final EntityScopeService getEntityScopeService() {
        Set<EntityScope> scopes = ImmutableSet.of(new DefaultEntityScope(), new ImmutableEntityScope());
        return new EntityScopeService(scopes);
    }

    private static final class ImmutableEntityScope implements EntityScope {

        @Override
        public String getName() {
            return "immutable_entity_scope_test";
        }

        @Override
        public boolean isMutable() {
            return false;
        }
    }
}
