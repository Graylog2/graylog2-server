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
package org.graylog2.inputs.persistence;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.Ints;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.inputs.InputService;
import org.graylog2.rest.models.system.inputs.responses.InputDeleted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * This is a simple wrapper around MongoDB to allow storage of state data for stateful Inputs.
 *
 * Inputs using this service are responsible for defining their own model for InputStatusRecord.inputStateData
 */
@Singleton
public class MongoInputStatusService implements InputStatusService {
    private static final Logger LOG = LoggerFactory.getLogger(MongoInputStatusService.class);

    public static final String COLLECTION_NAME = "input_status";

    private final InputService inputService;
    private final MongoCollection<InputStatusRecord> collection;

    @Inject
    public MongoInputStatusService(MongoCollections mongoCollections, InputService inputService, EventBus eventBus) {
        this.inputService = inputService;
        this.collection = mongoCollections.nonEntityCollection(COLLECTION_NAME, InputStatusRecord.class);

        eventBus.register(this);
    }

    @Override
    public Optional<InputStatusRecord> get(final String inputId) {
        return Optional.ofNullable(collection.find(MongoUtils.idEq(inputId)).first());
    }

    @Override
    public InputStatusRecord save(InputStatusRecord statusRecord) {
        if (statusRecord.inputId() == null) {
            final var insertedId = MongoUtils.insertedIdAsString(collection.insertOne(statusRecord));
            return statusRecord.toBuilder().inputId(insertedId).build();
        }
        collection.replaceOne(MongoUtils.idEq(statusRecord.inputId()), statusRecord, new ReplaceOptions().upsert(true));
        return statusRecord;
    }

    @Override
    public int delete(String inputId) {
        return Ints.saturatedCast(collection.deleteOne(MongoUtils.idEq(inputId)).getDeletedCount());
    }

    /**
     * Clean up MongoDB records when Inputs are deleted
     *
     * At the moment, Graylog uses the InputDeleted event both when an Input is stopped
     * and when it is deleted.
     *
     * @param event ID of the input being deleted
     */
    @Subscribe
    public void handleInputDeleted(InputDeleted event) {
        LOG.debug("Input Deleted event received for Input [{}]", event.id());

        // The input system is currently sending an "InputDeleted" event when an input gets deleted AND when an
        // input gets stopped. Check the database if the input was only stopped or actually deleted.
        // TODO: Remove this workaround once https://github.com/Graylog2/graylog2-server/issues/7812 is fixed
        try {
            inputService.find(event.id());
            // The input still exists so it only has been stopped. Don't do anything.
        } catch (NotFoundException e) {
            // The input is actually gone (deleted) so we can remove the state.
            LOG.debug("Deleting state for input <{}> from database", event.id());
            delete(event.id());
        }
    }
}
