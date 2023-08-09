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
package org.graylog2.cluster.preflight;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class DataNodeProvisioningBindings extends AbstractModule {

    @Override
    protected void configure() {
        // this wires the NodePreflightConfigServiceImpl delegate into the NodePreflightConfigBusEvents from above
        bind(DataNodeProvisioningService.class).annotatedWith(Names.named(DataNodeProvisioningBusEvents.DELEGATE_NAME)).to(DataNodeProvisioningServiceImpl.class);

        // this is the generic dependency used by callers
        bind(DataNodeProvisioningService.class).to(DataNodeProvisioningBusEvents.class);
    }
}
