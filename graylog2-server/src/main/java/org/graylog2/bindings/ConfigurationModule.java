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
package org.graylog2.bindings;

import com.google.inject.Binder;
import com.google.inject.Module;
import org.graylog2.Configuration;
import org.graylog2.plugin.BaseConfiguration;

import static java.util.Objects.requireNonNull;

public class ConfigurationModule implements Module {
    private final Configuration configuration;

    public ConfigurationModule(Configuration configuration) {
        this.configuration = requireNonNull(configuration);
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(Configuration.class).toInstance(configuration);
        binder.bind(BaseConfiguration.class).toInstance(configuration);
    }
}
