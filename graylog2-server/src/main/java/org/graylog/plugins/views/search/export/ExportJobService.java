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

import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.joda.time.DateTime;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import java.util.Optional;

public class ExportJobService {
    protected final JacksonDBCollection<ExportJob, ObjectId> db;

    @Inject
    public ExportJobService(MongoConnection mongoConnection,
                            MongoJackObjectMapperProvider mapper) {
        db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("export_jobs"),
                ExportJob.class,
                ObjectId.class,
                mapper.get());

    }

    public Optional<ExportJob> get(String id) {
        return Optional.ofNullable(db.findOneById(new ObjectId(id)));
    }

    public String save(ExportJob exportJob) {
        final WriteResult<ExportJob, ObjectId> save = db.insert(exportJob);

        return save.getSavedId().toHexString();
    }

    public void removeExpired(DateTime olderThan) {
        db.remove(DBQuery.lessThan(ExportJob.FIELD_CREATED_AT, olderThan));
    }
}
