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
package org.graylog2.system.stats.mongo;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;

import javax.annotation.Nullable;

/**
 * @see <a href="http://docs.mongodb.org/manual/reference/command/serverStatus/">Diagnostic Commands &gt; serverStatus</a>
 */
@JsonAutoDetect
@AutoValue
public abstract class ServerStatus {
    @JsonProperty("host")
    public abstract String host();

    @JsonProperty("version")
    public abstract String version();

    @JsonProperty("process")
    public abstract String process();

    @JsonProperty("pid")
    public abstract long pid();

    @JsonProperty("uptime")
    public abstract int uptime();

    @JsonProperty("uptime_millis")
    public abstract long uptimeMillis();

    @JsonProperty("uptime_estimate")
    public abstract int uptimeEstimate();

    @JsonProperty("local_time")
    public abstract DateTime localTime();

    @JsonProperty("connections")
    public abstract Connections connections();

    @JsonProperty("network")
    public abstract Network network();

    @JsonProperty("memory")
    public abstract Memory memory();

    @JsonProperty("storage_engine")
    public abstract StorageEngine storageEngine();

    public static ServerStatus create(String host,
                                      String version,
                                      String process,
                                      long pid,
                                      int uptime,
                                      long uptimeMillis,
                                      int uptimeEstimate,
                                      DateTime localTime,
                                      Connections connections,
                                      Network network,
                                      Memory memory,
                                      StorageEngine storageEngine) {
        return new AutoValue_ServerStatus(host, version, process, pid, uptime, uptimeMillis, uptimeEstimate, localTime,
                connections, network, memory, storageEngine);
    }

    @JsonAutoDetect
    @AutoValue
    public abstract static class Connections {
        @JsonProperty("current")
        public abstract int current();

        @JsonProperty("available")
        public abstract int available();

        @JsonProperty("total_created")
        @Nullable
        public abstract Long totalCreated();

        public static Connections create(int current,
                                         int available,
                                         @Nullable Long totalCreated) {
            return new AutoValue_ServerStatus_Connections(current, available, totalCreated);
        }
    }

    @JsonAutoDetect
    @AutoValue
    public abstract static class Network {
        @JsonProperty("bytes_in")
        public abstract int bytesIn();

        @JsonProperty("bytes_out")
        public abstract int bytesOut();

        @JsonProperty("num_requests")
        public abstract int numRequests();

        public static Network create(int bytesIn,
                                     int bytesOut,
                                     int numRequests) {
            return new AutoValue_ServerStatus_Network(bytesIn, bytesOut, numRequests);
        }
    }

    @JsonAutoDetect
    @AutoValue
    public abstract static class Memory {
        @JsonProperty("bits")
        public abstract int bits();

        @JsonProperty("resident")
        public abstract int resident();

        @JsonProperty("virtual")
        public abstract int virtual();

        @JsonProperty("supported")
        public abstract boolean supported();

        @JsonProperty("mapped")
        public abstract int mapped();

        @JsonProperty("mapped_with_journal")
        public abstract int mappedWithJournal();

        public static Memory create(int bits,
                                    int resident,
                                    int virtual,
                                    boolean supported,
                                    int mapped,
                                    int mappedWithJournal) {
            return new AutoValue_ServerStatus_Memory(bits, resident, virtual, supported, mapped, mappedWithJournal);
        }
    }

    @JsonAutoDetect
    @AutoValue
    public abstract static class StorageEngine {
        public static final StorageEngine DEFAULT = create("mmapv1");

        @JsonProperty("name")
        public abstract String name();

        public static StorageEngine create(String name) {
            return new AutoValue_ServerStatus_StorageEngine(name);
        }
    }

    // TODO Implement the remaining information from serverStatus?
}
