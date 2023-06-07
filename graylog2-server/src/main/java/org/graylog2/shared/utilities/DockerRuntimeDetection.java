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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class DockerRuntimeDetection {

    public static Boolean isRunningInsideDocker() {
        try (Stream<String> stream = Files.lines(Paths.get("/proc/self/cgroup"))) {
            // only works with cgroup v1
            if (stream.anyMatch(line -> line.contains("/docker"))) {
                return true;
            }
        } catch (IOException ignored) {
        }
        // this should work on cgroup v2
        try (Stream<String> stream = Files.lines(Paths.get("/proc/self/mountinfo"))) {
            if (stream.anyMatch(line -> line.contains("/docker/containers"))) {
                return true;
            }
        } catch (IOException ignored) {
        }
        // Last attempt to detect docker
        return Files.exists(Path.of("/.dockerenv"));
    }
}
