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
