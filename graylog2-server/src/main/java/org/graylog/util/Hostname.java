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
package org.graylog.util;

import com.google.auto.value.AutoValue;

import java.net.InetAddress;

import static java.util.Objects.requireNonNull;

@AutoValue
public abstract class Hostname {
    /**
     * Returns the short hostname.
     *
     * @return the hostname
     * @see InetAddress#getHostName()
     */
    public abstract String hostname();

    /**
     * Returns the full hostname.
     *
     * @return the hostname
     * @see InetAddress#getCanonicalHostName()
     */
    public abstract String canonicalHostname();

    /**
     * Creates the hostname object.
     *
     * @param hostname          the short hostname, see: {@link InetAddress#getHostName()}
     * @param canonicalHostname the full hostname, see: {@link InetAddress#getCanonicalHostName()}
     * @return the newly created hostname object
     */
    public static Hostname create(String hostname, String canonicalHostname) {
        return new AutoValue_Hostname(requireNonNull(hostname), requireNonNull(canonicalHostname));
    }
}
