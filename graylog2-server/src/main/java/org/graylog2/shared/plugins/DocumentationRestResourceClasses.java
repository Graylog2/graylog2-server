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

import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.rest.PluginRestResource;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;
import java.util.Set;

/**
 * This class provides access to the system and plugin API resources that are available through the Guice multi-binder.
 *
 * We need this wrapper class to be able to inject the {@literal Set<Class<? extends PluginRestResource>>} and
 * the named system resources into a Jersey REST resource. HK2 does not allow to inject this directly into the
 * resource class.
 */
public class DocumentationRestResourceClasses {
    private final Map<String, Set<Class<? extends PluginRestResource>>> pluginRestResources;
    private final Set<Class<?>> systemRestResources;

    @Inject
    public DocumentationRestResourceClasses(final Map<String, Set<Class<? extends PluginRestResource>>> pluginRestResources,
                                            @Named(Graylog2Module.SYSTEM_REST_RESOURCES) final Set<Class<?>> systemResources) {
        this.pluginRestResources = pluginRestResources;
        this.systemRestResources = systemResources;
    }

    /**
     * Returns a map of plugin package names to Sets of {@link PluginRestResource} classes.
     *
     * @return the map
     */
    public Map<String, Set<Class<? extends PluginRestResource>>> getPluginResourcesMap() {
        return pluginRestResources;
    }

    /**
     * Returns all system resources.
     *
     * @return the set of system resource classes
     */
    public Set<Class<?>> getSystemResources() {
        return systemRestResources;
    }
}
