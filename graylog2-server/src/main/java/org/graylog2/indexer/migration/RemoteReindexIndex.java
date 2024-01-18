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
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import java.util.concurrent.TimeUnit;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RemoteReindexIndex(String name, Status status, DateTime created, Duration took, Integer batches,
                                 String error) {
    public RemoteReindexIndex(String name, Status status) {
        this(name, status, DateTime.now(DateTimeZone.UTC), null, null, null);
    }

    public RemoteReindexIndex(String name, Status status, DateTime created, Duration took, Integer batches) {
        this(name, status, created, took, batches, null);
    }

    public RemoteReindexIndex(String name, Status status, DateTime created) {
        this(name, status, created, null, null, null);
    }

    public RemoteReindexIndex(String name, Status status, DateTime created, String error) {
        this(name, status, created, null, null, error);
    }

    public RemoteReindexIndex(String name, Status status, String error) {
        this(name, status, DateTime.now(DateTimeZone.UTC), null, null, error);
    }
}
