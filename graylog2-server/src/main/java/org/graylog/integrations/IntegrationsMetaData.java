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
package org.graylog.integrations;

import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

/**
 * Implement the PluginMetaData interface here.
 */
public class IntegrationsMetaData implements PluginMetaData {
    private static final String PLUGIN_PROPERTIES = "org.graylog.plugins.graylog-plugin-integrations/graylog-plugin.properties";

    @Override
    public String getUniqueId() {
        return "org.graylog.integrations.IntegrationsPlugin";
    }

    @Override
    public String getName() {
        return "Integrations";
    }

    @Override
    public String getAuthor() {
        return "Graylog, Inc. <hello@graylog.com>";
    }

    @Override
    public URI getURL() {
        return URI.create("https://github.com/Graylog2/graylog-plugin-integrations.git");
    }

    @Override
    public Version getVersion() {
        return Version.CURRENT_CLASSPATH;
    }

    @Override
    public String getDescription() {
        // TODO Insert correct plugin description
        return "A collection of plugins that integrate external systems with Graylog.";
    }

    @Override
    public Version getRequiredVersion() {
        return Version.CURRENT_CLASSPATH;
    }

    @Override
    public Set<ServerStatus.Capability> getRequiredCapabilities() {
        return Collections.emptySet();
    }
}
