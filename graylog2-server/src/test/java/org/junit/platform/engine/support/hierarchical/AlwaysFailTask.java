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
package org.junit.platform.engine.support.hierarchical;

import org.junit.jupiter.api.Assertions;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.TestDescriptor;

public class AlwaysFailTask<C extends EngineExecutionContext> implements HierarchicalTestExecutorService.TestTask {
    private final NodeTestTaskContext taskContext;
    private final TestDescriptor testDescriptor;
    private final Node<C> node;

    public AlwaysFailTask(NodeTestTaskContext taskContext, TestDescriptor testDescriptor) {
        this.taskContext = taskContext;
        this.testDescriptor = testDescriptor;
        this.node = NodeUtils.asNode(testDescriptor);
    }

    public ResourceLock getResourceLock() {
        return this.taskContext.getExecutionAdvisor().getResourceLock(this.testDescriptor);
    }

    public Node.ExecutionMode getExecutionMode() {
        return this.taskContext.getExecutionAdvisor().getForcedExecutionMode(this.testDescriptor).orElse(this.node.getExecutionMode());
    }

    @Override
    public void execute() {
//        throw new JUnitException("Backend unavailable. Maybe a container startup failure?");
        Assertions.fail("Backend unavailable. Maybe a container startup failure?");
    }
}
