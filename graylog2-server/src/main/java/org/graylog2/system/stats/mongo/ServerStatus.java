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
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.DateTime;

import javax.annotation.Nullable;

/**
 * @see <a href="http://docs.mongodb.org/manual/reference/command/serverStatus/">Diagnostic Commands &gt; serverStatus</a>
 */
@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class ServerStatus {
    @JsonProperty
    public abstract String host();

    @JsonProperty
    public abstract String version();

    @JsonProperty
    public abstract String process();

    @JsonProperty
    public abstract long pid();

    @JsonProperty
    public abstract int uptime();

    @JsonProperty
    public abstract long uptimeMillis();

    @JsonProperty
    public abstract int uptimeEstimate();

    @JsonProperty
    public abstract DateTime localTime();

    @JsonProperty
    public abstract Connections connections();

    @JsonProperty
    public abstract Network network();

    @JsonProperty
    public abstract Memory memory();

    @JsonProperty
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
    @WithBeanGetter
    public abstract static class Connections {
        @JsonProperty
        public abstract int current();

        @JsonProperty
        public abstract int available();

        @JsonProperty
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
    @WithBeanGetter
    public abstract static class Network {
        @JsonProperty
        public abstract int bytesIn();

        @JsonProperty
        public abstract int bytesOut();

        @JsonProperty
        public abstract int numRequests();

        public static Network create(int bytesIn,
                                     int bytesOut,
                                     int numRequests) {
            return new AutoValue_ServerStatus_Network(bytesIn, bytesOut, numRequests);
        }
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public abstract static class Memory {
        @JsonProperty
        public abstract int bits();

        @JsonProperty
        public abstract int resident();

        @JsonProperty
        public abstract int virtual();

        @JsonProperty
        public abstract boolean supported();

        @JsonProperty
        public abstract int mapped();

        @JsonProperty
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
    @WithBeanGetter
    public abstract static class StorageEngine {
        public static final StorageEngine DEFAULT = create("mmapv1");

        @JsonProperty
        public abstract String name();

        public static StorageEngine create(String name) {
            return new AutoValue_ServerStatus_StorageEngine(name);
        }
    }

    // TODO Implement the remaining information from serverStatus?
}
