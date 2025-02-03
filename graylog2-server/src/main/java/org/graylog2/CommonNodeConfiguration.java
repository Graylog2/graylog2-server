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
public interface CommonNodeConfiguration extends GraylogNodeConfiguration {

    @Override
    default boolean withMongoDb() {
        return true;
    }

    @Override
    default boolean withScheduler() {
        return true;
    }

    @Override
    default boolean withEventBus() {
        return true;
    }

    @Override
    default boolean withPlugins() {
        return false;
    }

    @Override
    default boolean withNodeIdFile() {
        return true;
    }

    @Override
    default boolean withInputs() {
        return false;
    }

    @Override
    default Set<ServerStatus.Capability> withCapabilities() {
        return Set.of(ServerStatus.Capability.SERVER);
    }

    @Override
    default boolean isMessageRecordingsEnabled() {
        return false;
    }

    @Override
    default String getEnvironmentVariablePrefix() {
        return "GRAYLOG_";
    }

    @Override
    default String getSystemPropertyPrefix() {
        return "graylog.";
    }
}
