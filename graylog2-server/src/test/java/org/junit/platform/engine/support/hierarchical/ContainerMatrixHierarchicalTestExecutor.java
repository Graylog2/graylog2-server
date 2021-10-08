/*
 * Copyright 2015-2020 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.GraylogBackend;
import org.junit.jupiter.engine.descriptor.ContainerMatrixEngineDescriptor;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;

import java.util.Optional;
import java.util.concurrent.Future;

public class ContainerMatrixHierarchicalTestExecutor<C extends EngineExecutionContext> {

    private final ExecutionRequest request;
    private final C rootContext;
    private final HierarchicalTestExecutorService executorService;
    private final ThrowableCollector.Factory throwableCollectorFactory;
    private final TestDescriptor testDescriptor;
    public static Optional<GraylogBackend> graylogBackend = Optional.empty();
    public static Optional<RequestSpecification> requestSpecification = Optional.empty();

    ContainerMatrixHierarchicalTestExecutor(ExecutionRequest request, C rootContext, HierarchicalTestExecutorService executorService,
                                            ThrowableCollector.Factory throwableCollectorFactory, TestDescriptor testDescriptor, GraylogBackend backend,
                                            RequestSpecification spec) {
        this.request = request;
        this.rootContext = rootContext;
        this.executorService = executorService;
        this.throwableCollectorFactory = throwableCollectorFactory;
        this.testDescriptor = testDescriptor;
        graylogBackend = Optional.of(backend);
        requestSpecification = Optional.of(spec);
    }

    Future<Void> execute() {
        ContainerMatrixEngineDescriptor rd = (ContainerMatrixEngineDescriptor) request.getRootTestDescriptor();
        ContainerMatrixEngineDescriptor rootTestDescriptor = new ContainerMatrixEngineDescriptor(rd.getUniqueId(),
                rd.getDisplayName(), rd.getConfiguration());
        rootTestDescriptor.addChildren(testDescriptor.getChildren());

        EngineExecutionListener executionListener = this.request.getEngineExecutionListener();
        NodeExecutionAdvisor executionAdvisor = new NodeTreeWalker().walk(rootTestDescriptor);
        NodeTestTaskContext taskContext = new NodeTestTaskContext(executionListener, this.executorService,
                this.throwableCollectorFactory, executionAdvisor);
        NodeTestTask<C> rootTestTask = new NodeTestTask<>(taskContext, rootTestDescriptor);
        rootTestTask.setParentContext(this.rootContext);
        return this.executorService.submit(rootTestTask);
    }

}
