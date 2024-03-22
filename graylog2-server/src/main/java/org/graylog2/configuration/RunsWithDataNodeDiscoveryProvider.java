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
package org.graylog2.configuration;

import com.google.common.base.Suppliers;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;

import java.util.function.Supplier;


public class RunsWithDataNodeDiscoveryProvider implements Provider<Boolean> {

    private final NodeService<DataNodeDto> nodeService;

    private final Supplier<Boolean> resultsCachingSupplier;

    @Inject
    public RunsWithDataNodeDiscoveryProvider(
            NodeService<DataNodeDto> nodeService) {
        this.nodeService = nodeService;
        this.resultsCachingSupplier = Suppliers.memoize(this::doGet);
    }

    @Override
    public Boolean get() {
        return resultsCachingSupplier.get();
    }

    private Boolean doGet() {
        return !nodeService.allActive().isEmpty();
    }
}
