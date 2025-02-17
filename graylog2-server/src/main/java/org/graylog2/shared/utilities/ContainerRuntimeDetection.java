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
package org.graylog2.shared.utilities;

import com.google.common.annotations.VisibleForTesting;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ContainerRuntimeDetection {

    public static Boolean isRunningInsideContainer() {
        return isRunningInsideContainer(
                () -> Files.lines(Paths.get("/proc/self/cgroup")),
                () -> Files.lines(Paths.get("/proc/self/mountinfo")),
                () -> Stream.of(Path.of("/.dockerenv"), Path.of("/run/.containerenv"))
        );
    }

    @VisibleForTesting
    static Boolean isRunningInsideContainer(Callable<Stream<String>> cgroupV1Lines,
                                            Callable<Stream<String>> cgroupV2Lines,
                                            Supplier<Stream<Path>> canaryPaths) {
        try (Stream<String> stream = cgroupV1Lines.call()) {
            // only works with cgroup v1
            if (stream.anyMatch(line -> line.contains("/docker"))) {
                return true;
            }
        } catch (Exception ignored) {
        }
        // this should work on cgroup v2
        try (Stream<String> stream = cgroupV2Lines.call()) {
            // We expect that all container instances have /etc/hosts mounted with a container-runtime-specific
            // path in the "/proc/self/mountinfo" file.
            //
            // Example values for different runtimes:
            // - Kubernetes: "4443 4434 259:1 /var/lib/kubelet/pods/2015a4b3-a05b-4e4d-8fa3-699914c33ee8/etc-hosts /etc/hosts rw,noatime - xfs /dev/nvme0n1p1 rw,seclabel,attr2,inode64,logbufs=8,logbsize=32k,sunit=1024,swidth=1024,noquota"
            // - Docker: "2098 2061 252:1 /var/lib/docker/containers/3ff5651b246362e1d9d5f2070f5d5988ffca7b8922afe0d75e4fae6e505d6597/hosts /etc/hosts rw,relatime - ext4 /dev/mapper/vgubuntu-root rw,errors=remount-ro"
            if (stream.anyMatch(line -> line.matches(".+/(?:docker/containers|kubelet/pods)/.+/etc/hosts.+"))) {
                return true;
            }
        } catch (Exception ignored) {
        }
        // Last attempt to detect that we are running inside a container.
        return canaryPaths.get().anyMatch(Files::exists);
    }
}
