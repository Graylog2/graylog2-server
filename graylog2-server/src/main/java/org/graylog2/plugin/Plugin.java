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
package org.graylog2.plugin;

import java.util.Collection;

/**
 * A graylog plugin.
 * <p>
 * Core configuration beans like {@link org.graylog2.Configuration} are made available to plugins via member injection.
 * If you need access to them, you can define a field of the required bean type and annotate it with
 *
 * @{@link javax.inject.Inject} as in the following example:
 * <pre>
 * {@code
 * public class MyPlugin implements Plugin {
 *     @Inject
 *     private Configuration configuration;
 *
 *     @Override
 *     public PluginMetaData metadata() {
 *         return new MyPluginMetaData();
 *     }
 *
 *     @Override
 *     public Collection<PluginModule> modules() {
 *         return Collections.singletonList(new MyPluginModule(configuration));
 *     }
 * }
 * }
 * </pre>
 * </p>
 */
public interface Plugin {
    PluginMetaData metadata();

    Collection<PluginModule> modules();
}
