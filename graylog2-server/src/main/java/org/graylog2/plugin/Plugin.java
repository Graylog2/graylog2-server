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

public interface Plugin {
    PluginMetaData metadata();

    Collection<PluginModule> modules();

    /**
     * If you need access to the base configuration to configure your bindings, override this method.
     */
    default Collection<PluginModule> modules(BaseConfiguration configuration) {
        return modules();
    }
}
