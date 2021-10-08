package org.junit.jupiter.engine.discovery;

import org.graylog.testing.containermatrix.discovery.IsContainerMatrixTestClass;
import org.junit.jupiter.engine.descriptor.ContainerMatrixEngineDescriptor;
import org.junit.jupiter.engine.descriptor.ContainerMatrixTestsDescriptor;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.discovery.EngineDiscoveryRequestResolver;

public class ContainerMatrixTestsDiscoverySelectorResolver {
    private final ContainerMatrixEngineDescriptor engineDescriptor;

    public ContainerMatrixTestsDiscoverySelectorResolver(ContainerMatrixEngineDescriptor engineDescriptor) {
        this.engineDescriptor = engineDescriptor;
    }

    // add all Tests to the intermediate ContainerMatrixTestsDescriptor container
    public void resolveSelectors(final EngineDiscoveryRequest request, final ContainerMatrixTestsDescriptor testsDescriptor) {
        // @formatter:off
        final EngineDiscoveryRequestResolver<ContainerMatrixTestsDescriptor> resolver = EngineDiscoveryRequestResolver.<ContainerMatrixTestsDescriptor>builder()
                .addClassContainerSelectorResolver(new IsContainerMatrixTestClass(testsDescriptor))
                .addSelectorResolver(context -> new ContainerMatrixClassSelectorResolver(context.getClassNameFilter(), engineDescriptor.getConfiguration(), testsDescriptor))
                .addSelectorResolver(context -> new ContainerMatrixMethodSelectorResolver(engineDescriptor.getConfiguration(), testsDescriptor))
                .addTestDescriptorVisitor(context -> new MethodOrderingVisitor(engineDescriptor.getConfiguration()))
                .addTestDescriptorVisitor(context -> TestDescriptor::prune)
                .build();
        // @formatter:on

        resolver.resolve(request, testsDescriptor);
    }
}
