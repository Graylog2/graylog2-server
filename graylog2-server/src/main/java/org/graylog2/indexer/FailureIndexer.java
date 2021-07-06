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
package org.graylog2.indexer;

import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.messages.IndexingRequest;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

import static org.graylog2.plugin.streams.Stream.FAILURES_STREAM_ID;

/**
 * This class contains indices helper for the events system.
 */
@Singleton
public class FailureIndexer {
    private static final Logger LOG = LoggerFactory.getLogger(FailureIndexer.class);

    private final StreamService streamService;
    private final Messages messages;

    @Inject
    public FailureIndexer(StreamService streamService, Messages messages) {
        this.streamService = streamService;
        this.messages = messages;
    }

    public void write(List<FailureObject> failureObjects) {
        if (failureObjects.isEmpty()) {
            return;
        }

        IndexSet failuresIndexSet;
        try {
            failuresIndexSet = streamService.load(FAILURES_STREAM_ID).getIndexSet();
        } catch (NotFoundException e) {
            e.printStackTrace();
            return;
        }

        final List<IndexingRequest> requests = failureObjects.stream()
                .map(failureObject -> IndexingRequest.create(failuresIndexSet, failureObject))
                .collect(Collectors.toList());
        messages.bulkIndexRequests(requests, true);
    }

}
