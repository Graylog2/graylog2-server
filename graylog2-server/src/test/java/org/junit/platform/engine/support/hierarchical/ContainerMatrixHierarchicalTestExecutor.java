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

import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.GraylogBackend;
import org.junit.jupiter.engine.descriptor.ContainerMatrixEngineDescriptor;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Future;

public class ContainerMatrixHierarchicalTestExecutor<C extends EngineExecutionContext> {

    private final ExecutionRequest request;
    private final C rootContext;
    private final HierarchicalTestExecutorService executorService;
    private final ThrowableCollector.Factory throwableCollectorFactory;
    private final Collection<? extends TestDescriptor> testDescriptors;
    public static Optional<GraylogBackend> graylogBackend = Optional.empty();
    public static Optional<RequestSpecification> requestSpecification = Optional.empty();

    ContainerMatrixHierarchicalTestExecutor(ExecutionRequest request, C rootContext, HierarchicalTestExecutorService executorService,
                                            ThrowableCollector.Factory throwableCollectorFactory, Collection<? extends TestDescriptor> testDescriptors, GraylogBackend backend,
                                            RequestSpecification spec) {
        this.request = request;
        this.rootContext = rootContext;
        this.executorService = executorService;
        this.throwableCollectorFactory = throwableCollectorFactory;
        this.testDescriptors = testDescriptors;
        graylogBackend = Optional.of(backend);
        requestSpecification = Optional.of(spec);
    }

    Future<Void> execute() {
        ContainerMatrixEngineDescriptor rd = (ContainerMatrixEngineDescriptor) request.getRootTestDescriptor();
        ContainerMatrixEngineDescriptor rootTestDescriptor = new ContainerMatrixEngineDescriptor(rd.getUniqueId(),
                rd.getDisplayName(), rd.getConfiguration());
        rootTestDescriptor.addChildren(testDescriptors);

        EngineExecutionListener executionListener = this.request.getEngineExecutionListener();
        NodeExecutionAdvisor executionAdvisor = new NodeTreeWalker().walk(rootTestDescriptor);
        NodeTestTaskContext taskContext = new NodeTestTaskContext(executionListener, this.executorService,
                this.throwableCollectorFactory, executionAdvisor);
        NodeTestTask<C> rootTestTask = new NodeTestTask<>(taskContext, rootTestDescriptor);
        rootTestTask.setParentContext(this.rootContext);
        return this.executorService.submit(rootTestTask);
    }

}
