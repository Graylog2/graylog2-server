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
import org.graylog2.plugin.Message;
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
     * Returns an id of a failed message. The id is represented
     * by a value of the {@link Message#FIELD_GL2_MESSAGE_ID} field
     *
     * TODO: currently it's false for processing failures !!!
     */
    String failedMessageId();

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
     * Returns a subcategory of this failure
     */
    String errorType();

    /**
     * Returns a detailed error message
     */
    String errorMessage();

    /**
     * Returns a timestamp of the failed message
     */
    DateTime timestamp();

    /**
     * Returns true if the failed message must
     * be acknowledged upon failure handling
     */
    boolean requiresAcknowledgement();
}
