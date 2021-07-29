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
/**
 * The interface is designed to deliver "necessary" information about a failure occurring during
 * message processing (e.g. Elasticsearch indexing, extraction, pipeline processing and etc).
 *
 * The eventual destination are implementations of {@link org.graylog.failure.FailureHandler}, whose
 * role is making this information available for further troubleshooting / failure recovery.
 */
public interface Failure {

    FailureType failureType();

    String failedMessageId();

    Indexable failedMessage();

    @Nullable
    String targetIndex();

    String context();

    String errorMessage();

    DateTime timestamp();

    boolean requiresAcknowledgement();
}
