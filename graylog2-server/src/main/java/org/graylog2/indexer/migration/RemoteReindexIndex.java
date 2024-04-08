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
package org.graylog2.indexer.migration;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.graylog2.indexer.datanode.RemoteReindexingMigrationAdapter.Status;
import org.joda.time.DateTime;
import org.joda.time.Duration;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RemoteReindexIndex(String name, Status status, DateTime created, Duration took,
                                 IndexMigrationProgress progress, String errorMsg) {


    public static RemoteReindexIndex notStartedYet(String indexName) {
        return new RemoteReindexIndex(indexName, Status.NOT_STARTED, null, null, null, null);
    }

}
