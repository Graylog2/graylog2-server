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
package org.graylog2.shared.rest.resources.csp;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog.security.events.AuthServiceBackendSavedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class CSPEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(CSPEventListener.class);
    private final CSPService cspService;

    @Inject
    public CSPEventListener(EventBus eventBus,
                            CSPService cspService) {
        this.cspService = cspService;
        eventBus.register(this);
    }

    @Subscribe
    public void handleBackendCreated(AuthServiceBackendSavedEvent event) {
        try {
            LOG.debug("Updating CSP for authentication service <{}>", event.authServiceId());
            cspService.updateConnectSrc();
        } catch (Exception e) {
            LOG.warn("Failed to update CSP for authentication service <{}>", event.authServiceId(), e);
        }
    }
}
