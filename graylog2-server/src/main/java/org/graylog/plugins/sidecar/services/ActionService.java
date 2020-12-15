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
package org.graylog.plugins.sidecar.services;

import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog.plugins.sidecar.rest.models.CollectorAction;
import org.graylog.plugins.sidecar.rest.models.CollectorActions;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class ActionService {
    private static final String COLLECTION_NAME = "sidecar_collector_actions";
    private final JacksonDBCollection<CollectorActions, ObjectId> dbCollection;

    @Inject
    public ActionService(MongoConnection mongoConnection,
                         MongoJackObjectMapperProvider mapper){
        dbCollection = JacksonDBCollection.wrap(
                mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
                CollectorActions.class,
                ObjectId.class,
                mapper.get());
    }

    public CollectorActions fromRequest(String sidecarId, List<CollectorAction> actions) {
        CollectorActions collectorActions = findActionBySidecar(sidecarId, false);
        if (collectorActions == null) {
            return CollectorActions.create(
                    sidecarId,
                    DateTime.now(DateTimeZone.UTC),
                    actions);
        }
        List<CollectorAction> updatedActions = new ArrayList<>();
        for (final CollectorAction action : actions) {
            for (final CollectorAction existingsAction : collectorActions.action()) {
                if (!existingsAction.collectorId().equals(action.collectorId())) {
                    updatedActions.add(existingsAction);
                }
            }
            updatedActions.add(action);
        }
        return CollectorActions.create(
                collectorActions.id(),
                sidecarId,
                DateTime.now(DateTimeZone.UTC),
                updatedActions);
    }

    public CollectorActions saveAction(CollectorActions collectorActions) {
        return dbCollection.findAndModify(
                DBQuery.is("sidecar_id", collectorActions.sidecarId()),
                new BasicDBObject(),
                new BasicDBObject(),
                false,
                collectorActions,
                true,
                true);
    }

    public CollectorActions findActionBySidecar(String sidecarId, boolean remove) {
        if (remove) {
            return dbCollection.findAndRemove(DBQuery.is("sidecar_id", sidecarId));
        } else {
            return dbCollection.findOne(DBQuery.is("sidecar_id", sidecarId));
        }
    }
}
