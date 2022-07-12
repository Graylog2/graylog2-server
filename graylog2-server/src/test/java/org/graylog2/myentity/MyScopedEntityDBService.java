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
package org.graylog2.myentity;

import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.entities.EntityScopeService;
import org.graylog2.database.entities.ScopedEntityDbService;

import javax.inject.Inject;

/**
 * Specific MyEntity DB service implementation.
 */
public class MyScopedEntityDBService extends ScopedEntityDbService<MyScopedEntity> {

    public static final String COLLECTION_NAME = "my_entities";

    @Inject
    public MyScopedEntityDBService(MongoConnection mongoConnection,
                                   MongoJackObjectMapperProvider mapper,
                                   EntityScopeService entityScopeService) {
        super(mongoConnection, mapper, MyScopedEntity.class, COLLECTION_NAME, entityScopeService);
    }
}
