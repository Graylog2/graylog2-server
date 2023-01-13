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
package org.graylog2.shared.initializers;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager.Listener;
import org.graylog2.plugin.ServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ServiceManagerListener extends Listener {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceManagerListener.class);
    private final ServerStatus serverStatus;

    @Inject
    public ServiceManagerListener(ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
    }

    @Override
    public void healthy() {
        LOG.info("Services are healthy");
        serverStatus.start();
    }

    @Override
    public void stopped() {
        LOG.info("Services are now stopped.");
    }

    @Override
    public void failure(Service service) {
        // do not log the failure here again, the ServiceManager itself does so already on Level ERROR.
        serverStatus.fail();
    }
}
