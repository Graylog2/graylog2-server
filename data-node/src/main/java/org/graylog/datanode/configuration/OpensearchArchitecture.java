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
package org.graylog.datanode.configuration;

public enum OpensearchArchitecture {
    x64,
    aarch64;

    public static OpensearchArchitecture fromOperatingSystem() {
        final var osArch = System.getProperty("os.arch");
        return fromCode(osArch);
    }

    public static OpensearchArchitecture fromCode(String osArch) {
        return switch (osArch) {
            case "amd64" -> x64;
            case "x86_64" -> x64;
            case "x64" -> x64;
            case "aarch64" -> aarch64;
            case "arm64" -> aarch64;
            default ->
                    throw new UnsupportedOperationException("Unsupported OpenSearch distribution architecture: " + osArch);
        };
    }
}
