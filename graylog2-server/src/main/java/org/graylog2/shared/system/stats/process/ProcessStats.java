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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class ProcessStats {
    @JsonProperty
    public abstract long pid();

    @JsonProperty
    public abstract long openFileDescriptors();

    @JsonProperty
    public abstract long maxFileDescriptors();

    @JsonProperty
    @Nullable
    public abstract Cpu cpu();

    @JsonProperty
    @Nullable
    public abstract Memory memory();

    public static ProcessStats create(long pid,
                                      long openFileDescriptors,
                                      long maxFileDescriptors,
                                      Cpu cpu,
                                      Memory memory) {
        return new AutoValue_ProcessStats(pid, openFileDescriptors, maxFileDescriptors, cpu, memory);
    }

    public static ProcessStats create(long pid,
                                      long openFileDescriptors,
                                      long maxFileDescriptors) {
        return create(pid, openFileDescriptors, maxFileDescriptors, null, null);
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public abstract static class Cpu {
        @JsonProperty
        public abstract short percent();

        @JsonProperty
        public abstract long sys();

        @JsonProperty
        public abstract long user();

        @JsonProperty
        public abstract long total();

        public static Cpu create(short percent,
                                 long sys,
                                 long user,
                                 long total) {
            return new AutoValue_ProcessStats_Cpu(percent, sys, user, total);
        }
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public abstract static class Memory {
        @JsonProperty
        public abstract long totalVirtual();

        @JsonProperty
        public abstract long resident();

        @JsonProperty
        public abstract long share();

        public static Memory create(long totalVirtual,
                                    long resident,
                                    long share) {
            return new AutoValue_ProcessStats_Memory(totalVirtual, resident, share);
        }
    }
}
