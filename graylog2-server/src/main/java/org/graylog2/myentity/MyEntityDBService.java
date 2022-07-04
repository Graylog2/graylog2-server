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
import org.graylog2.database.entities.EntityDbService;

import javax.inject.Inject;

/**
 * Specific MyEntity DB service implementation.
 */
public class MyEntityDBService extends EntityDbService<MyEntity> {

    public static final String COLLECTION_NAME = "my_entities";

    @Inject
    public MyEntityDBService(MongoConnection mongoConnection, MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, MyEntity.class, COLLECTION_NAME);
    }
}
