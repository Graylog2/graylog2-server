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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.ReturnDocument;
import jakarta.inject.Inject;
import org.graylog.plugins.sidecar.rest.models.CollectorAction;
import org.graylog.plugins.sidecar.rest.models.CollectorActions;
import org.graylog2.database.MongoCollections;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

public class ActionService {
    private static final String COLLECTION_NAME = "sidecar_collector_actions";

    private final MongoCollection<CollectorActions> collection;
    private final EtagService etagService;

    @Inject
    public ActionService(MongoCollections mongoCollections,
                         EtagService etagService) {
        this.etagService = etagService;
        collection = mongoCollections.collection(COLLECTION_NAME, CollectorActions.class);
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
        final var actions = collection.findOneAndReplace(
                eq("sidecar_id", collectorActions.sidecarId()),
                collectorActions,
                new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(true)
        );
        etagService.invalidateRegistration(collectorActions.sidecarId());
        return actions;
    }

    public CollectorActions findActionBySidecar(String sidecarId, boolean remove) {
        if (remove) {
            return collection.findOneAndDelete(eq("sidecar_id", sidecarId));
        } else {
            return collection.find(eq("sidecar_id", sidecarId)).first();
        }
    }
}
