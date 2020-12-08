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
package org.graylog2.shared.plugins;

import org.graylog2.plugin.rest.PluginRestResource;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

/**
 * This class provides the map of {@link PluginRestResource} classes that are available through the Guice mapbinder.
 *
 * We need this wrapper class to be able to inject the {@literal Set<Class<? extends PluginRestResource>>} into
 * a Jersey REST resource. HK2 does not allow to inject this directly into the resource class.
 */
public class PluginRestResourceClasses {
    private final Map<String, Set<Class<? extends PluginRestResource>>> pluginRestResources;

    @Inject
    public PluginRestResourceClasses(final Map<String, Set<Class<? extends PluginRestResource>>> pluginRestResources) {
        this.pluginRestResources = pluginRestResources;
    }

    /**
     * Returns a map of plugin packge names to Sets of {@link PluginRestResource} classes.
     *
     * @return the map
     */
    public Map<String, Set<Class<? extends PluginRestResource>>> getMap() {
        return pluginRestResources;
    }
}
