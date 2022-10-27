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
import org.junit.platform.commons.util.UnrecoverableExceptions;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AlwaysFailTask<C extends EngineExecutionContext> implements HierarchicalTestExecutorService.TestTask {
    private final NodeTestTaskContext taskContext;
    private final TestDescriptor testDescriptor;
    private final Node<C> node;
    private ThrowableCollector throwableCollector;
    private C parentContext;
    private C context;

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

    public void setParentContext(C parentContext) {
        this.parentContext = parentContext;
    }

    private void prepare() {
        this.throwableCollector.execute(() -> {
            this.context = this.node.prepare(this.parentContext);
        });
        this.parentContext = null;
    }

    private void cleanUp() {
        this.throwableCollector.execute(() -> {
            this.node.cleanUp(this.context);
        });
    }

    private void reportCompletion() {
        this.node.nodeFinished(this.context, this.testDescriptor, TestExecutionResult.failed(new AssertionError("Backend unavailable. Maybe a container startup failure?")));

        this.taskContext.getListener().executionFinished(this.testDescriptor, TestExecutionResult.failed(new AssertionError("Backend unavailable. Maybe a container startup failure?")));
        this.throwableCollector = null;
    }

    @Override
    public void execute() {
        this.throwableCollector = this.taskContext.getThrowableCollectorFactory().create();

        this.prepare();

        this.throwableCollector.execute(() -> {
            List<AlwaysFailTask<C>> children = (List) this.testDescriptor.getChildren().stream().map((descriptor) -> {
                return new AlwaysFailTask(this.taskContext, descriptor);
            }).collect(Collectors.toCollection(ArrayList::new));
            if (!children.isEmpty()) {
                children.forEach((child) -> {
                    child.setParentContext(this.context);
                });
                this.taskContext.getExecutorService().invokeAll(children);
            }
        });

        if (this.context != null) {
            this.cleanUp();
        }

        this.reportCompletion();
    }
}
