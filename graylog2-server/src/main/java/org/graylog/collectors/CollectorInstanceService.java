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
package org.graylog.collectors;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.conversions.Bson;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.collectors.db.CollectorInstanceReport;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.pagination.MongoPaginationHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;
import static com.mongodb.client.model.Updates.setOnInsert;

@Singleton
public class CollectorInstanceService {

    private final MongoCollection<CollectorInstanceDTO> collection;
    private final MongoPaginationHelper<CollectorInstanceDTO> paginationHelper;

    @Inject
    public CollectorInstanceService(MongoCollections mongoCollections) {
        collection = mongoCollections.collection("collector_instances", CollectorInstanceDTO.class);
        paginationHelper = mongoCollections.paginationHelper(collection);
    }

    /**
     * Saves an incoming collector instance report and returns the previously saved state if available.
     *
     * @param update the report to save
     * @return optionally the previous version of the report
     */
    public Optional<CollectorInstanceDTO> createOrUpdateFromReport(CollectorInstanceReport update) {
        final List<Bson> updateOps = new ArrayList<>();

        updateOps.add(setOnInsert("instance_uid", update.instanceUid()));
        updateOps.add(set("last_seen", update.lastSeen()));
        updateOps.add(set("message_seq_num", update.messageSeqNum()));
        if (update.identifyingAttributes().isPresent()) {
            updateOps.add(set("identifying_attributes", update.identifyingAttributes().get()));
        }
        if (update.nonIdentifyingAttributes().isPresent()) {
            updateOps.add(set("non_identifying_attributes", update.nonIdentifyingAttributes().get()));
        }

        // we request the ReturnDocument.BEFORE here to avoid having to load the previous document in full just
        // to retrieve the previous `message_seq_num`, which we need to determine what to do next.
        // the result is not the full CollectorInstanceDTO as we have it, but the minimal set of fields necessary to
        // determine next steps
        return Optional.ofNullable(collection.findOneAndUpdate(Filters.eq("instance_uid", update.instanceUid()),
                combine(updateOps),
                new FindOneAndUpdateOptions()
                        .returnDocument(ReturnDocument.BEFORE)
                        .projection(Projections.include("instance_uid", "message_seq_num", "last_seen", "capabilities", "txn_cursor"))
                        .upsert(true)));
    }
}
