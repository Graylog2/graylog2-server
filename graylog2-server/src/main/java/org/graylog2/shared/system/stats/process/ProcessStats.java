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
package org.graylog2.shared.system.stats.process;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@JsonAutoDetect
@AutoValue
public abstract class ProcessStats {
    @JsonProperty("pid")
    public abstract long pid();

    @JsonProperty("open_file_descriptors")
    public abstract long openFileDescriptors();

    @JsonProperty("max_file_descriptors")
    public abstract long maxFileDescriptors();

    @JsonProperty("cpu")
    @Nullable
    public abstract Cpu cpu();

    @JsonProperty("memory")
    @Nullable
    public abstract Memory memory();

    @JsonCreator
    public static ProcessStats create(@JsonProperty("pid") long pid,
                                      @JsonProperty("open_file_descriptors") long openFileDescriptors,
                                      @JsonProperty("max_file_descriptors") long maxFileDescriptors,
                                      @JsonProperty("cpu") Cpu cpu,
                                      @JsonProperty("memory") Memory memory) {
        return new AutoValue_ProcessStats(pid, openFileDescriptors, maxFileDescriptors, cpu, memory);
    }

    public static ProcessStats create(long pid,
                                      long openFileDescriptors,
                                      long maxFileDescriptors) {
        return create(pid, openFileDescriptors, maxFileDescriptors, null, null);
    }

    @JsonAutoDetect
    @AutoValue
    public abstract static class Cpu {
        @JsonProperty("percent")
        public abstract short percent();

        @JsonProperty("sys")
        public abstract long sys();

        @JsonProperty("user")
        public abstract long user();

        @JsonProperty("total")
        public abstract long total();

        @JsonCreator
        public static Cpu create(@JsonProperty("percent") short percent,
                                 @JsonProperty("sys") long sys,
                                 @JsonProperty("user") long user,
                                 @JsonProperty("total") long total) {
            return new AutoValue_ProcessStats_Cpu(percent, sys, user, total);
        }
    }

    @JsonAutoDetect
    @AutoValue
    public abstract static class Memory {
        @JsonProperty("total_virtual")
        public abstract long totalVirtual();

        @JsonProperty("resident")
        public abstract long resident();

        @JsonProperty("share")
        public abstract long share();

        @JsonCreator
        public static Memory create(@JsonProperty("total_virtual") long totalVirtual,
                                    @JsonProperty("resident") long resident,
                                    @JsonProperty("share") long share) {
            return new AutoValue_ProcessStats_Memory(totalVirtual, resident, share);
        }
    }
}
