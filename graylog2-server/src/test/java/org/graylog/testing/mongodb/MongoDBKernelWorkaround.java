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
package org.graylog.testing.mongodb;

import com.google.common.primitives.Ints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * Workaround for running MongoDB 8.x (including the "latest" tag, which currently resolves to an 8.x
 * release) on Linux kernel version >= 6.19.
 * See: https://jira.mongodb.org/browse/SERVER-121912
 */
public final class MongoDBKernelWorkaround {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDBKernelWorkaround.class);

    private MongoDBKernelWorkaround() {
    }

    public static <T extends GenericContainer<?>> T applyIfNeeded(T container, String dockerImageName) {
        try {
            final var versionPart = DockerImageName.parse(dockerImageName).getVersionPart();
            if (versionPart.startsWith("8.") || versionPart.equals("latest")) {
                final var osName = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);

                if (osName.contains("linux")) {
                    final var kernelVersion = System.getProperty("os.version", "0.0").split("\\.");
                    if (kernelVersion.length < 2) {
                        throw new IllegalStateException("Unexpected Linux kernel version: " + Arrays.toString(kernelVersion));
                    }
                    final int kernelMajorVersion = Objects.requireNonNullElse(Ints.tryParse(kernelVersion[0]), 0);
                    final int kernelMinorVersion = Objects.requireNonNullElse(Ints.tryParse(kernelVersion[1]), 0);

                    if (kernelMajorVersion >= 7 || (kernelMajorVersion == 6 && kernelMinorVersion >= 19)) {
                        LOG.info("Applying MongoDB 8.x workaround (running on Linux {})", System.getProperty("os.version"));
                        container.withEnv("GLIBC_TUNABLES", "glibc.pthread.rseq=1");
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Error applying Linux kernel version workaround for MongoDB 8.x", e);
        }
        return container;
    }
}
