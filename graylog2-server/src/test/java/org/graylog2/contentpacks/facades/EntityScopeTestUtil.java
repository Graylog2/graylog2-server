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

    private static final class ImmutableEntityScope extends EntityScope {

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
