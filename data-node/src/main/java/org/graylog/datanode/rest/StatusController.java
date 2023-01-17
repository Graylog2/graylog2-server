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
package org.graylog.datanode.rest;

import org.graylog.datanode.management.ManagedNodes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class StatusController {

    @Value("${datanode.version}")
    private String dataNodeVersion;

    @Autowired
    private ManagedNodes openSearch;

    @GetMapping("/")
    public DataNodeStatus status() {

        return openSearch.getProcesses()
                .stream()
                .map(process -> new StatusResponse(process.getOpensearchVersion(), process.getProcessInfo()))
                .collect(Collectors.collectingAndThen(Collectors.toList(), statusResponses -> new DataNodeStatus(dataNodeVersion, statusResponses)));
    }
}
