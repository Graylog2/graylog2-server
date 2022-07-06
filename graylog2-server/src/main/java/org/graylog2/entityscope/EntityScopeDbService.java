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
package org.graylog2.entityscope;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import javax.inject.Inject;
import java.util.Optional;

public class EntityScopeDbService extends PaginatedDbService<EntityScope> {
    private static final String COLLECTION_NAME = "entity_scope";

    @Inject
    public EntityScopeDbService(MongoConnection mongoConnection,
                                MongoJackObjectMapperProvider mapper) {
        super(mongoConnection,
                mapper,
                EntityScope.class,
                COLLECTION_NAME);
    }

    public PaginatedList<EntityScope> findAll(int page, int pageSize) {
        return findPaginatedWithQueryAndSort(DBQuery.empty(),
                DBSort.asc(EntityScope.FIELD_TITLE),
                page,
                pageSize);
    }

    public Optional<EntityScope> findByName(String name) {

        DBObject query = new BasicDBObject(EntityScope.FIELD_TITLE, name);
        return Optional.ofNullable(db.findOne(query));
    }

}
