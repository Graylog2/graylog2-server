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

import com.google.inject.Injector;
import org.graylog2.bootstrap.preflight.PreflightCheckException;

/**
 * This Module can be implemented by Plugins that wish to perform
 * preflight checks before the server is started.
 * <p>
 * The Module serves two tasks:
 * <ul>
 *     Set up a minimal Guice Binding that will be used to create an injector
 *     to create the instances needed to perform preflight checks.
 * </ul>
 * <ul>
 *     Run the actual preflight checks by implementing {@link PreflightCheckModule#doCheck(Injector)}
 *     The Injector can be used to create the necessary test instances.
 * </ul>
 */
public abstract class PreflightCheckModule extends PluginModule {
    public abstract void doCheck(Injector injector) throws PreflightCheckException;
}
