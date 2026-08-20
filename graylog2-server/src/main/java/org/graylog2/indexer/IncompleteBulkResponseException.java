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

/**
 * Thrown by a {@code MessagesAdapter} when an indexer's bulk response cannot be parsed because it omitted
 * shard-failure detail while a shard was being reallocated, e.g. during a rolling restart. Retried like other
 * transient indexing failures, see {@code Messages#createBulkRequestRetryerBuilder()}.
 * <p>
 * See <a href="https://github.com/opensearch-project/opensearch-java/issues/551">opensearch-java#551</a>: the
 * client treats the shard number on a shard-failure entry as a required field, but OpenSearch can omit it while
 * the failing shard is still being promoted. A client-side fix is proposed in
 * <a href="https://github.com/opensearch-project/opensearch-java/pull/2023">opensearch-java#2023</a> (unmerged
 * as of this writing); once a released client version includes it, this workaround can likely be removed.
 */
public class IncompleteBulkResponseException extends ElasticsearchException {
    public IncompleteBulkResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
