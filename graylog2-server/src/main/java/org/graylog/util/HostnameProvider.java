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

import org.graylog2.plugin.Tools;

import javax.inject.Provider;

public class HostnameProvider implements Provider<Hostname> {
    /**
     * Returns the {@link Hostname} object for the local node.
     *
     * @return the hostname
     * @throws RuntimeException when local hostname cannot be retrieved
     */
    @Override
    public Hostname get() {
        try {
            return Hostname.create(Tools.getLocalHostname(), Tools.getLocalCanonicalHostname());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
