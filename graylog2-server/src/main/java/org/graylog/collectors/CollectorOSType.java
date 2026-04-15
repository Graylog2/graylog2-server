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
package org.graylog.collectors;

import java.util.Arrays;

import static org.graylog2.shared.utilities.StringUtils.requireNonBlank;

/**
 * An enum for Collector operating systems.
 */
public enum CollectorOSType {
    LINUX("linux"),
    MACOS("darwin"),
    UNKNOWN("unknown"),
    WINDOWS("windows");

    private final String osName;

    CollectorOSType(String osName) {
        this.osName = osName;
    }

    /**
     * Returns the operating system type for the given name. Returns UNKNOWN when the given name doesn't match any type.
     *
     * @param osName the operating system name
     * @return the operating system type for the given string or UNKNOWN
     */
    public static CollectorOSType of(String osName) {
        requireNonBlank(osName, "osName can't be blank");

        return Arrays.stream(CollectorOSType.values())
                .filter(type -> type.osName.equals(osName))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
