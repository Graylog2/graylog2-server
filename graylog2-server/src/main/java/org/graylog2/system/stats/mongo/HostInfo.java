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
 * @see <a href="http://docs.mongodb.org/manual/reference/command/hostInfo/">Diagnostic Commands &gt; hostInfo</a>
 */
@JsonAutoDetect
@AutoValue
public abstract class HostInfo {
    @JsonProperty("system")
    public abstract System system();

    @JsonProperty("os")
    public abstract Os os();

    @JsonProperty("extra")
    public abstract Extra extra();

    public static HostInfo create(System system,
                                  Os os,
                                  Extra extra) {
        return new AutoValue_HostInfo(system, os, extra);
    }

    @JsonAutoDetect
    @AutoValue
    @SuppressWarnings("JavaLangClash")
    public abstract static class System {
        @JsonProperty("current_time")
        public abstract DateTime currentTime();

        @JsonProperty("hostname")
        public abstract String hostname();

        @JsonProperty("cpu_addr_size")
        public abstract int cpuAddrSize();

        @JsonProperty("mem_size_mb")
        public abstract long memSizeMB();

        @JsonProperty("num_cores")
        public abstract int numCores();

        @JsonProperty("cpu_arch")
        public abstract String cpuArch();

        @JsonProperty("numa_enabled")
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
    public abstract static class Os {
        @JsonProperty("type")
        public abstract String type();

        @JsonProperty("name")
        public abstract String name();

        @JsonProperty("version")
        public abstract String version();

        public static Os create(String type,
                                String name,
                                String version) {
            return new AutoValue_HostInfo_Os(type, name, version);
        }
    }

    @JsonAutoDetect
    @AutoValue
    public abstract static class Extra {
        @JsonProperty("version_string")
        @Nullable
        public abstract String versionString();

        @JsonProperty("libc_version")
        @Nullable
        public abstract String libcVersion();

        @JsonProperty("kernel_version")
        @Nullable
        public abstract String kernelVersion();

        @JsonProperty("cpu_frequency_mhz")
        @Nullable
        public abstract String cpuFrequencyMHz();

        @JsonProperty("cpu_features")
        @Nullable
        public abstract String cpuFeatures();

        @JsonProperty("scheduler")
        @Nullable
        public abstract String scheduler();

        @JsonProperty("page_size")
        public abstract Long pageSize();

        @JsonProperty("num_pages")
        @Nullable
        public abstract Long numPages();

        @JsonProperty("max_open_files")
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
