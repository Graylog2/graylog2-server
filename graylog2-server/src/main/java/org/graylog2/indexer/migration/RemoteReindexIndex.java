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

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RemoteReindexIndex {
    private final String name;
    private Status status;
    private final DateTime created;
    private Duration took;
    private Integer batches;
    private String errorMsg;

    public RemoteReindexIndex(final String name, final Status status) {
        this.name = name;
        this.status = status;
        this.created = DateTime.now(DateTimeZone.UTC);
    }

    public String getName() {
        return name;
    }

    public Status getStatus() {
        return status;
    }


    public DateTime getCreated() {
        return created;
    }

    public Duration getTook() {
        return took;
    }


    public Integer getBatches() {
        return batches;
    }


    public String getErrorMsg() {
        return errorMsg;
    }

    public void onError(String errorMsg) {
        this.status = Status.ERROR;
        this.errorMsg = errorMsg;
    }

    public void onFinished(Duration duration, int batches) {
        this.status = Status.FINISHED;
        this.took = duration;
        this.batches = batches;
    }

    @Override
    public String toString() {
        return "{" +
                "name='" + name + '\'' +
                ", status=" + status +
                '}';
    }
}
