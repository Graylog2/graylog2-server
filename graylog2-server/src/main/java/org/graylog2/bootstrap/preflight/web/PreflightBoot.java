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
package org.graylog2.bootstrap.preflight.web;

import org.graylog2.Configuration;
import org.graylog2.bootstrap.preflight.PreflightConfig;
import org.graylog2.bootstrap.preflight.PreflightConfigResult;
import org.graylog2.bootstrap.preflight.PreflightConfigService;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.net.URI;
import java.util.List;

@Singleton
public class PreflightBoot {

    private final boolean isFreshInstallation;
    private final List<URI> hosts;
    private final PreflightConfigService preflightConfigServiceIf;
    private final Configuration configuration;

    @Inject
    public PreflightBoot(@Named("isFreshInstallation") boolean isFreshInstallation, @Named("elasticsearch_hosts") List<URI> hosts,
                         PreflightConfigService preflightConfigServiceIf, Configuration configuration) {
        this.isFreshInstallation = isFreshInstallation;
        this.hosts = hosts;
        this.preflightConfigServiceIf = preflightConfigServiceIf;
        this.configuration = configuration;
    }

    public boolean shouldRunPreflightWeb() {

        if (configuration.enablePreflightWebserver()) {
            return true;
        }

        if (!isFreshInstallation) {
            return false;
        }

        if (!hosts.isEmpty()) {
            return false;
        }

        if (preflightFinishedOrSkipped()) {
            return false;
        }

        return true;
    }

    private boolean preflightFinishedOrSkipped() {
        final PreflightConfigResult result = preflightConfigServiceIf.getPreflightConfigResult();
        return result == PreflightConfigResult.FINISHED || result == PreflightConfigResult.SKIPPED;
    }
}
