package org.graylog.testing.containermatrix;

import org.junit.jupiter.engine.config.CachingJupiterConfiguration;
import org.junit.jupiter.engine.config.DefaultJupiterConfiguration;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ContainerMatrixEngineDescriptor;
import org.junit.jupiter.engine.discovery.ContainerMatrixTestsDiscoverySelectorResolver;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.support.JupiterThrowableCollectorFactory;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutorService;
import org.junit.platform.engine.support.hierarchical.ThrowableCollector;

public class AltContainerMatrixTextEngine extends HierarchicalTestEngine<JupiterEngineExecutionContext> {
    private static final String ENGINE_ID = "graylog-container-matrix-tests";

    @Override
    public String getId() {
        return ENGINE_ID;
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        JupiterConfiguration configuration = new CachingJupiterConfiguration(
                new DefaultJupiterConfiguration(discoveryRequest.getConfigurationParameters()));
        final ContainerMatrixEngineDescriptor engineDescriptor = new ContainerMatrixEngineDescriptor(uniqueId, "Graylog Container Matrix Tests", configuration);
        new ContainerMatrixTestsDiscoverySelectorResolver().resolveSelectors(discoveryRequest, engineDescriptor);
        return engineDescriptor;
    }

    @Override
    protected HierarchicalTestExecutorService createExecutorService(ExecutionRequest request) {
        // TODO: eigenen ExecutorService, der ggfls. Backends erzeugt
        return super.createExecutorService(request);
    }

    @Override
    protected JupiterEngineExecutionContext createExecutionContext(ExecutionRequest request) {
        return new JupiterEngineExecutionContext(request.getEngineExecutionListener(),
                getJupiterConfiguration(request));
    }

    @Override
    protected ThrowableCollector.Factory createThrowableCollectorFactory(ExecutionRequest request) {
        return JupiterThrowableCollectorFactory::createThrowableCollector;
    }

    private JupiterConfiguration getJupiterConfiguration(ExecutionRequest request) {
        ContainerMatrixEngineDescriptor engineDescriptor = (ContainerMatrixEngineDescriptor) request.getRootTestDescriptor();
        return engineDescriptor.getConfiguration();
    }
}
