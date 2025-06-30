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
package org.graylog.datanode.bootstrap.preflight.inits;

import jakarta.inject.Inject;

import java.util.Set;

/**
 * Datanode init procedures that should be triggered after preflight but before the real injection and server startup
 * takes place.
 */
public class DatanodeBlockingInitService {
    private final Set<DatanodeBlockingInit> inits;

    @Inject
    public DatanodeBlockingInitService(Set<DatanodeBlockingInit> inits) {
        this.inits = inits;
    }

    public void runInits() {
        inits.forEach(DatanodeBlockingInit::runInit);
    }
}
