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

    /**
     * This will load bindings required for basic database connection and mongojack infrastructure.
     */
    boolean withMongoDb();

    /**
     * Binds the scheduled executors for daemon and non-daemon usage in the node.
     */
    boolean withScheduler();

    /**
     * Binds event bus and cluster event bus.
     */
    boolean withEventBus();

    /**
     * Configures node startup to load plugin configurations and plugins.
     */
    boolean withPlugins();

    /**
     * Will bind NodeId to an id provided by FilePersistedNodeIdProvider.
     * Falls back to use a dummy node id if set to 'false'.
     */
    boolean withNodeIdFile();

    /**
     * Will only bind an InputConfigurationDeserializerModifier stub if there are no inputs configured
     */
    boolean withInputs();

    /**
     * Provides the {@link ServerStatus.Capability} to be used by ServerStatusBindings.
     */
    Set<ServerStatus.Capability> withCapabilities();

    /**
     * Environment variable prefix to be used for this node (e.g. <pre>GRAYLOG_</pre> for Graylog nodes).
     */
    String getEnvironmentVariablePrefix();

    /**
     * System property prefix to be used for this node (e.g. <pre>graylog.</pre> for Graylog nodes).
     */
    String getSystemPropertyPrefix();

    /**
     * Enables message recording in ServerStatus.
     */
    boolean isMessageRecordingsEnabled();
}
