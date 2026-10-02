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
package org.graylog.datanode.metrics;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.stream.Stream;

/**
 * Reads memory and CPU usage of the cgroup (v2) the Data Node and its OpenSearch process are running in.
 * OpenSearch's own node stats only support cgroup v1, so on cgroup v2 hosts (e.g. Docker Desktop, current
 * Kubernetes distributions) they do not contain any container information.
 * <p>
 * The cgroup interface files are only present for non-root cgroups, so outside of containers nothing is reported.
 */
public class CgroupV2Reader {

    private static final Path DEFAULT_CGROUP_ROOT = Path.of("/sys/fs/cgroup");

    private final Path cgroupRoot;
    private final Clock clock;

    public CgroupV2Reader() {
        this(DEFAULT_CGROUP_ROOT, Clock.systemUTC());
    }

    CgroupV2Reader(Path cgroupRoot, Clock clock) {
        this.cgroupRoot = cgroupRoot;
        this.clock = clock;
    }

    /**
     * The used memory is calculated like {@code docker stats} does: {@code memory.current - inactive_file}, so that
     * reclaimable page cache is not reported as used.
     */
    public Optional<CgroupMemory> readMemory() {
        final OptionalLong current = readFirstLine("memory.current").map(CgroupV2Reader::parseLong).orElse(OptionalLong.empty());
        if (current.isEmpty() || current.getAsLong() < 0) {
            return Optional.empty();
        }
        final long inactiveFile = readStat("memory.stat", "inactive_file").orElse(0L);
        final long usedBytes = Math.max(0L, current.getAsLong() - inactiveFile);
        final OptionalLong limitBytes = readFirstLine("memory.max").map(CgroupV2Reader::parseLong).orElse(OptionalLong.empty());
        return Optional.of(new CgroupMemory(usedBytes, limitBytes));
    }

    public Optional<CgroupCpu> readCpu() {
        final OptionalLong usageMicros = readStat("cpu.stat", "usage_usec");
        if (usageMicros.isEmpty() || usageMicros.getAsLong() < 0) {
            return Optional.empty();
        }
        return Optional.of(new CgroupCpu(usageMicros.getAsLong() * 1_000L, clock.millis(), readCpuLimit()));
    }

    /**
     * cpu.max contains "$QUOTA $PERIOD", with "max" as quota if the CPU usage is not limited.
     */
    private OptionalDouble readCpuLimit() {
        final List<String> parts = readFirstLine("cpu.max").map(line -> List.of(line.trim().split("\\s+"))).orElse(List.of());
        if (parts.size() != 2) {
            return OptionalDouble.empty();
        }
        final OptionalLong quota = parseLong(parts.get(0));
        final OptionalLong period = parseLong(parts.get(1));
        if (quota.isEmpty() || period.isEmpty() || quota.getAsLong() <= 0 || period.getAsLong() <= 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of((double) quota.getAsLong() / period.getAsLong());
    }

    private OptionalLong readStat(String file, String key) {
        final String prefix = key + " ";
        try (Stream<String> lines = Files.lines(cgroupRoot.resolve(file))) {
            return lines.filter(line -> line.startsWith(prefix))
                    .findFirst()
                    .map(line -> parseLong(line.substring(prefix.length())))
                    .orElse(OptionalLong.empty());
        } catch (IOException | UncheckedIOException e) {
            return OptionalLong.empty();
        }
    }

    private Optional<String> readFirstLine(String file) {
        try (Stream<String> lines = Files.lines(cgroupRoot.resolve(file))) {
            return lines.findFirst();
        } catch (IOException | UncheckedIOException e) {
            return Optional.empty();
        }
    }

    /**
     * Returns an empty value for non-numeric values like "max", which cgroup v2 uses for "no limit".
     */
    private static OptionalLong parseLong(String value) {
        try {
            return OptionalLong.of(Long.parseLong(value.trim()));
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
    }

    /**
     * @param usedBytes  memory used by the container, excluding reclaimable page cache
     * @param limitBytes the container's memory limit, empty if the container is not limited
     */
    public record CgroupMemory(long usedBytes, OptionalLong limitBytes) {}

    /**
     * @param usageNanos      total CPU time consumed by the container
     * @param timestampMillis when the usage was read
     * @param cpuLimit        the number of CPUs the container may use, empty if the container is not limited
     */
    public record CgroupCpu(long usageNanos, long timestampMillis, OptionalDouble cpuLimit) {}
}
