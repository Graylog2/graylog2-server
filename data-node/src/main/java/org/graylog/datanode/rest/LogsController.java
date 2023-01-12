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

import org.graylog.datanode.process.OpensearchLogs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/logs")
public class LogsController {

    private final OpensearchLogs opensearchLogs;

    @Autowired
    public LogsController(OpensearchLogs opensearchLogs) {
        this.opensearchLogs = opensearchLogs;
    }

    @GetMapping("/stdout")
    public List<String> getOpensearchStdout() {
        return opensearchLogs.getStdout();
    }

    @GetMapping("/stderr")
    public List<String> getOpensearchStderr() {
        return opensearchLogs.getStderr();
    }
}
