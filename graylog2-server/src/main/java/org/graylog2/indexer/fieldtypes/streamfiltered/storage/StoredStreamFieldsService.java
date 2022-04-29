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
package org.graylog2.indexer.fieldtypes.streamfiltered.storage;

import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.indexer.fieldtypes.streamfiltered.storage.model.StoredStreamFields;

import javax.inject.Inject;

public class StoredStreamFieldsService extends PaginatedDbService<StoredStreamFields> {

    static final String STREAM_FIELDS_MONGODB_COLLECTION = "stream_fields";


    @Inject
    protected StoredStreamFieldsService(final MongoConnection mongoConnection,
                                        final MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, StoredStreamFields.class, STREAM_FIELDS_MONGODB_COLLECTION);
    }
}
