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
package org.junit.jupiter.engine.descriptor;

import org.junit.platform.engine.TestDescriptor;

import java.net.URL;
import java.util.List;
import java.util.Set;

public class ContainerMatrixTestWithRunningESMongoTestsDescriptor extends ContainerMatrixTestsDescriptor {
    public ContainerMatrixTestWithRunningESMongoTestsDescriptor(TestDescriptor parent,
                                                                Set<Integer> extraPorts,
                                                                List<URL> mongoDBFixtures) {
        super(parent, "Testing with running ES and MongoDB instances.", extraPorts, mongoDBFixtures);
    }
}


