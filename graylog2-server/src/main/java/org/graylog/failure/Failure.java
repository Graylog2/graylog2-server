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
package org.graylog.failure;


import org.graylog2.indexer.messages.Indexable;
import org.joda.time.DateTime;

import javax.annotation.Nullable;

/**
 * A failure occurring at different stages of message processing
 * (e.g. pipeline processing, extraction, Elasticsearch indexing)
 */
public interface Failure {

    /**
     * Returns a type of this failure
     */
    FailureType failureType();

    /**
     * Returns a cause of this failure
     */
    FailureCause failureCause();

    /**
     * Returns a brief description of this failure, which
     * is supposed to answer the following 2 questions:
     *      1) WHAT has happened?
     *      2) WHICH component has caused it?
     */
    String message();

    /**
     * Returns further failure details, which are supposed
     * to answer the question "WHY this failure has happened?"
     */
    String failureDetails();

    /**
     * Returns a timestamp of this failure
     */
    DateTime failureTimestamp();

    /**
     * Returns a failed message
     */
    Indexable failedMessage();

    /**
     * Returns an ElasticSearch index name targeted by
     * the failed message. For non-indexing failures
     * the value might be null.
     */
    @Nullable
    String targetIndex();


    /**
     * Returns true if the failed message must
     * be acknowledged upon failure handling
     */
    boolean requiresAcknowledgement();
}
