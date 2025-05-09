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
package org.graylog2.datanode;

import jakarta.inject.Provider;
import org.graylog2.plugin.Version;

/**
 * Let's inject the server version where needed. Using the static CURRENT_CLASSPATH field is making our code untestable.
 * Injection allows clean unit tests.
 */
public class ServerVersionProvider implements Provider<Version> {
    @Override
    public Version get() {
        return Version.CURRENT_CLASSPATH;
    }
}
