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
package org.graylog.datanode.management;

import org.graylog.datanode.DataNodeRunner;
import org.graylog.datanode.process.OpensearchProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
@Scope("singleton")
public class ManagedNodes {

    private final Set<OpensearchProcess> processes = new LinkedHashSet<>();

    @Autowired
    private DataNodeRunner dataNodeRunner;

    @Autowired
    private ConfigurationProvider configurationProvider;

    @EventListener(ApplicationReadyEvent.class)
    public void startOpensearchProcesses() {
        configurationProvider.get()
                .stream()
                .map(dataNodeRunner::start)
                .forEach(processes::add);
    }

    public Set<OpensearchProcess> getProcesses() {
        return processes;
    }
}
