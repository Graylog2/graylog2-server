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
package org.graylog.datanode.bindings;

import com.google.common.util.concurrent.ServiceManager;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.shared.bindings.providers.ServiceManagerProvider;

public class GenericBindings extends Graylog2Module {
    private final boolean isMigrationCommand;

    public GenericBindings(boolean isMigrationCommand) {
        this.isMigrationCommand = isMigrationCommand;
    }

    @Override
    protected void configure() {
        bind(ServiceManager.class).toProvider(ServiceManagerProvider.class).asEagerSingleton();
    }

}
