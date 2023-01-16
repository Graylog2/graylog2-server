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
package org.graylog2.shared.bindings.providers;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Set;

public class ServiceManagerProvider implements Provider<ServiceManager> {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceManagerProvider.class);

    @Inject
    Set<Service> services = Sets.<Service>newHashSet(new AbstractService() {
        @Override
        protected void doStart() {
        }

        @Override
        protected void doStop() {

        }
    });

    @Override
    public ServiceManager get() {
        LOG.debug("Using services: {}", services);
        return new ServiceManager(services);
    }
}
