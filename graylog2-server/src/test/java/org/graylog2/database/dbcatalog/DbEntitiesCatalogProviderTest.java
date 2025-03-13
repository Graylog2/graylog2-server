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
package org.graylog2.database.dbcatalog;


import org.graylog2.cluster.nodes.ServerNodeEntity;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.users.UserImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.graylog2.database.DbEntity.NOBODY_ALLOWED;
import static org.graylog2.shared.security.RestPermissions.INDEXSETS_READ;
import static org.graylog2.shared.security.RestPermissions.USERS_READ;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
class DbEntitiesCatalogProviderTest {
    @Test
    void testHandlesEntitiesWithDefaultTitleFieldProperly() {
        DbEntitiesCatalogProvider provider = new DbEntitiesCatalogProvider(Set.of(IndexSetConfig.class));
        final DbEntitiesCatalog dbEntitiesCatalog = provider.get();

        final DbEntityCatalogEntry entryByCollectionName = dbEntitiesCatalog.getByCollectionName("index_sets").get();

        assertEquals(new DbEntityCatalogEntry("index_sets", "title", IndexSetConfig.class, INDEXSETS_READ), entryByCollectionName);
    }

    @Test
    void testHandlesEntitiesWithDefaultReadPermissionFieldProperly() {
        DbEntitiesCatalogProvider provider = new DbEntitiesCatalogProvider(Set.of(ServerNodeEntity.class));
        final DbEntitiesCatalog dbEntitiesCatalog = provider.get();

        final DbEntityCatalogEntry entryByCollectionName = dbEntitiesCatalog.getByCollectionName("nodes").get();

        assertEquals(new DbEntityCatalogEntry("nodes", "node_id", ServerNodeEntity.class, NOBODY_ALLOWED), entryByCollectionName);
    }

    @Test
    void testHandlesEntitiesWithCustomTitleFieldProperly() {
        DbEntitiesCatalogProvider provider = new DbEntitiesCatalogProvider(Set.of(UserImpl.class));
        final DbEntitiesCatalog dbEntitiesCatalog = provider.get();

        final DbEntityCatalogEntry entryByCollectionName = dbEntitiesCatalog.getByCollectionName(UserImpl.COLLECTION_NAME).get();

        assertEquals(new DbEntityCatalogEntry(UserImpl.COLLECTION_NAME, UserImpl.USERNAME, UserImpl.class, USERS_READ), entryByCollectionName);
    }
}
