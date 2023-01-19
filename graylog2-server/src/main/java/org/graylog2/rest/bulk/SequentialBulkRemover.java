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
package org.graylog2.rest.bulk;

import org.graylog2.rest.bulk.model.BulkDeleteRequest;
import org.graylog2.rest.bulk.model.BulkDeleteResponse;
import org.graylog2.rest.bulk.model.BulkOperationFailure;

import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of {@link BulkRemover} that removes entities sequentially, one at a time, using provided {@link SingleEntityRemover}.
 * It is meant for those type of entities that are very hard to remove with single MongoDB query in a single request,
 * as many additional checks are to be performed on the fetched entity in order to decide if it is legal to remove it at all...
 */
public class SequentialBulkRemover<T> implements BulkRemover<T> {

    static final BulkOperationFailure NO_ENTITY_IDS_FAILURE = new BulkOperationFailure("", "No IDs provided in the request");
    private final SingleEntityRemover<T> singleEntityRemover;

    public SequentialBulkRemover(final SingleEntityRemover<T> singleEntityRemover) {
        this.singleEntityRemover = singleEntityRemover;
    }

    @Override
    public BulkDeleteResponse bulkDelete(final BulkDeleteRequest bulkDeleteRequest, final T context) {
        if (bulkDeleteRequest.entityIds() == null || bulkDeleteRequest.entityIds().isEmpty()) {
            return new BulkDeleteResponse(0, List.of(NO_ENTITY_IDS_FAILURE));
        }

        List<BulkOperationFailure> capturedFailures = new LinkedList<>();
        for (String entityId : bulkDeleteRequest.entityIds()) {
            try {
                singleEntityRemover.remove(entityId, context);
            } catch (Exception ex) {
                capturedFailures.add(new BulkOperationFailure(entityId, ex.getMessage()));
            }
        }

        return new BulkDeleteResponse(
                bulkDeleteRequest.entityIds().size() - capturedFailures.size(),
                capturedFailures);
    }
}
