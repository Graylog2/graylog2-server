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
package org.graylog.grn;

import org.graylog.grn.providers.EventDefinitionGRNDescriptorProvider;
import org.graylog.grn.providers.EventNotificationGRNDescriptorProvider;
import org.graylog.grn.providers.FallbackGRNDescriptorProvider;
import org.graylog.grn.providers.StreamGRNDescriptorProvider;
import org.graylog.grn.providers.UserGRNDescriptorProvider;
import org.graylog.grn.providers.ViewGRNDescriptorProvider;
import org.graylog2.plugin.PluginModule;

public class GRNTypesModule extends PluginModule {
    @Override
    protected void configure() {
        // TODO: Implement missing GRN descriptor providers
        addGRNType(GRNTypes.BUILTIN_TEAM, FallbackGRNDescriptorProvider.class);
        addGRNType(GRNTypes.COLLECTION, FallbackGRNDescriptorProvider.class);
        addGRNType(GRNTypes.DASHBOARD, ViewGRNDescriptorProvider.class);
        addGRNType(GRNTypes.EVENT_DEFINITION, EventDefinitionGRNDescriptorProvider.class);
        addGRNType(GRNTypes.EVENT_NOTIFICATION, EventNotificationGRNDescriptorProvider.class);
        addGRNType(GRNTypes.GRANT, FallbackGRNDescriptorProvider.class);
        addGRNType(GRNTypes.ROLE, FallbackGRNDescriptorProvider.class);
        addGRNType(GRNTypes.SEARCH, ViewGRNDescriptorProvider.class);
        addGRNType(GRNTypes.STREAM, StreamGRNDescriptorProvider.class);
        addGRNType(GRNTypes.USER, UserGRNDescriptorProvider.class);
    }
}
