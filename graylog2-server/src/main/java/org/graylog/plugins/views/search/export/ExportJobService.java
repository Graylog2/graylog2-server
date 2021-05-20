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
package org.graylog.plugins.views.search.export;

import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ExportJobService {
    protected final JacksonDBCollection<ExportJob, ObjectId> db;

    @Inject
    public ExportJobService(MongoConnection mongoConnection,
                            MongoJackObjectMapperProvider mapper) {
        db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("export_jobs"),
                ExportJob.class,
                ObjectId.class,
                mapper.get());

        db.createIndex(new BasicDBObject(ExportJob.FIELD_CREATED_AT, 1), new BasicDBObject("expireAfterSeconds", TimeUnit.HOURS.toSeconds(1L)));
    }

    public Optional<ExportJob> get(String id) {
        if (!ObjectId.isValid(id)) {
            return Optional.empty();
        }
        return Optional.ofNullable(db.findOneById(new ObjectId(id)));
    }

    public String save(ExportJob exportJob) {
        final WriteResult<ExportJob, ObjectId> save = db.insert(exportJob);

        return save.getSavedId().toHexString();
    }
}
