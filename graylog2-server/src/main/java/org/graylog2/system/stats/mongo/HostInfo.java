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
 * @see <a href="http://docs.mongodb.org/manual/reference/command/hostInfo/">Diagnostic Commands &gt; hostInfo</a>
 */
@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class HostInfo {
    @JsonProperty
    public abstract System system();

    @JsonProperty
    public abstract Os os();

    @JsonProperty
    public abstract Extra extra();

    public static HostInfo create(System system,
                                  Os os,
                                  Extra extra) {
        return new AutoValue_HostInfo(system, os, extra);
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    @SuppressWarnings("JavaLangClash")
    public abstract static class System {
        @JsonProperty
        public abstract DateTime currentTime();

        @JsonProperty
        public abstract String hostname();

        @JsonProperty
        public abstract int cpuAddrSize();

        @JsonProperty
        public abstract long memSizeMB();

        @JsonProperty
        public abstract int numCores();

        @JsonProperty
        public abstract String cpuArch();

        @JsonProperty
        public abstract boolean numaEnabled();

        public static System create(DateTime currentTime,
                                    String hostname,
                                    int cpuAddrSize,
                                    long memSizeMB,
                                    int numCores,
                                    String cpuArch,
                                    boolean numaEnabled) {
            return new AutoValue_HostInfo_System(currentTime, hostname, cpuAddrSize, memSizeMB, numCores, cpuArch, numaEnabled);
        }
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public abstract static class Os {
        @JsonProperty
        public abstract String type();

        @JsonProperty
        public abstract String name();

        @JsonProperty
        public abstract String version();

        public static Os create(String type,
                                String name,
                                String version) {
            return new AutoValue_HostInfo_Os(type, name, version);
        }
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public abstract static class Extra {
        @JsonProperty
        @Nullable
        public abstract String versionString();

        @JsonProperty
        @Nullable
        public abstract String libcVersion();

        @JsonProperty
        @Nullable
        public abstract String kernelVersion();

        @JsonProperty
        @Nullable
        public abstract String cpuFrequencyMHz();

        @JsonProperty
        @Nullable
        public abstract String cpuFeatures();

        @JsonProperty
        @Nullable
        public abstract String scheduler();

        @JsonProperty
        public abstract Long pageSize();

        @JsonProperty
        @Nullable
        public abstract Long numPages();

        @JsonProperty
        @Nullable
        public abstract Long maxOpenFiles();

        public static Extra create(@Nullable String versionString,
                                   @Nullable String libcVersion,
                                   @Nullable String kernelVersion,
                                   @Nullable String cpuFrequencyMHz,
                                   @Nullable String cpuFeatures,
                                   @Nullable String scheduler,
                                   long pageSize,
                                   @Nullable Long numPages,
                                   @Nullable Long maxOpenFiles) {
            return new AutoValue_HostInfo_Extra(versionString, libcVersion, kernelVersion, cpuFrequencyMHz, cpuFeatures,
                    scheduler, pageSize, numPages, maxOpenFiles);
        }
    }
}
