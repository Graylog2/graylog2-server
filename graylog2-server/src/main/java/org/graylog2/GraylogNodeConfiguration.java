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
package org.graylog2;

import org.graylog2.plugin.ServerStatus;

import java.util.Set;

/**
 * Helper class to hold configuration shared by all Graylog node types
 */
public interface GraylogNodeConfiguration {

    boolean withMongoDb();

    boolean withScheduler();

    boolean withEventBus();

    boolean withPlugins();

    boolean withNodeIdFile();

    Set<ServerStatus.Capability> withCapabilities();

    String getEnvironmentVariablePrefix();

    String getSystemPropertyPrefix();

    boolean isMessageRecordingsEnabled();
}
