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
import org.graylog2.configuration.TelemetryConfiguration;
import org.graylog2.rest.PaginationParameters;

import javax.inject.Inject;
import java.util.stream.Collectors;

public class CSPServiceImpl implements CSPService {
    private final String telemetryApiHost;
    private final DBAuthServiceBackendService dbService;
    private String connectSrcValue;

    @Inject
    protected CSPServiceImpl(TelemetryConfiguration telemetryConfiguration, DBAuthServiceBackendService dbService) {
        this.telemetryApiHost = telemetryConfiguration.getTelemetryApiHost();
        this.dbService = dbService;
        buildConnectSrc();
    }

    @Override
    public void buildConnectSrc() {
        final String hostList = dbService.findPaginated(new PaginationParameters(), x -> true).stream()
                .map(dto -> String.join(" ", dto.config().hostAllowList()))
                .collect(Collectors.joining(" "));
        connectSrcValue = telemetryApiHost + " " + hostList;
    }

    @Override
    public String connectSrcValue() {
        return connectSrcValue;
    }
}
