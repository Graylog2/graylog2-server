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

import org.graylog.security.authservice.DBAuthServiceBackendService;
import org.graylog2.configuration.ContentStreamConfiguration;
import org.graylog2.configuration.TelemetryConfiguration;
import org.graylog2.rest.PaginationParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.net.URI;
import java.util.stream.Collectors;

public class CSPServiceImpl implements CSPService {
    private static final Logger LOG = LoggerFactory.getLogger(CSPServiceImpl.class);
    private final String telemetryApiHost;
    private final String contentStreamRssApiHost;
    private final DBAuthServiceBackendService dbService;
    private final CSPResources cspResources;

    @Inject
    protected CSPServiceImpl(TelemetryConfiguration telemetryConfiguration, ContentStreamConfiguration contentStreamConfiguration, DBAuthServiceBackendService dbService) {
        this.telemetryApiHost = telemetryConfiguration.getTelemetryApiHost();
        this.dbService = dbService;
        this.cspResources = new CSPResources();
        this.contentStreamRssApiHost = getContentStreamHost(contentStreamConfiguration.getContentStreamRssUri());
        updateConnectSrc();
    }

    @Override
    public synchronized void updateConnectSrc() {
        final String hostList = dbService.findPaginated(new PaginationParameters(), x -> true).stream()
                .map(dto -> dto.config().externalHTTPHosts())
                .filter(java.util.Optional::isPresent)
                .map(optList -> String.join(" ", optList.get()))
                .collect(Collectors.joining(" "));
        String connectSrcValue = "'self' " + telemetryApiHost + " " + contentStreamRssApiHost + " " + hostList;
        cspResources.updateAll("connect-src", connectSrcValue);
        LOG.debug("Updated CSP: {}", connectSrcValue);
    }

    @Override
    public String cspString(String group) {
        return cspResources.cspString(group);
    }

    private String getContentStreamHost(URI contentStreamRssApiHost) {
        String slash = "/";
        if (contentStreamRssApiHost.getPath().endsWith(slash)) {
            return contentStreamRssApiHost.toString();
        } else {
            return contentStreamRssApiHost.toString() + slash;
        }
    }
}
