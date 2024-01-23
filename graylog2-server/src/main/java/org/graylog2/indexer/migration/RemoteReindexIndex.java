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
public class RemoteReindexIndex {
    final String name;
    Status status;
    final DateTime created;
    Duration took;
    Integer batches;
    String errorMsg;

    public RemoteReindexIndex(final String name, final Status status) {
        this.name = name;
        this.status = status;
        this.created = DateTime.now(DateTimeZone.UTC);
    }

    public static RemoteReindexIndex createError(final String name, final String errorMsg) {
        var r = new RemoteReindexIndex(name, Status.ERROR);
        r.setErrorMsg(errorMsg);
        return r;
    }

    public static RemoteReindexIndex createFinished(String name, Duration duration, int batches) {
        var r = new RemoteReindexIndex(name, Status.FINISHED);
        r.setTook(duration);
        r.setBatches(batches);
        return r;
    }

    public String getName() {
        return name;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public DateTime getCreated() {
        return created;
    }

    public Duration getTook() {
        return took;
    }

    public void setTook(Duration took) {
        this.took = took;
    }

    public Integer getBatches() {
        return batches;
    }

    public void setBatches(Integer batches) {
        this.batches = batches;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
