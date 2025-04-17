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
package org.graylog2.storage.versionprobe;

import com.github.joschi.jadconfig.util.Duration;

public interface VersionProbeFactory {
    /**
     * Create a VersionProbe instance with all defaults from the graylog server configuration
     */
    VersionProbe createDefault();

    /**
     * Create instance with specific connection details and custom listener
     */
    VersionProbe create(int probeAttempts, Duration probeDelay, boolean useJwtAuthentication, VersionProbeListener versionProbeListener);
}
