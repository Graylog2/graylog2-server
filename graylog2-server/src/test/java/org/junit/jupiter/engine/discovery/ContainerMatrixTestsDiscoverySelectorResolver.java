package org.junit.jupiter.engine.discovery;

import org.graylog.testing.containermatrix.descriptors.ContainerMatrixTestsDescriptor;
import org.graylog.testing.containermatrix.discovery.IsContainerMatrixTestClass;
import org.junit.jupiter.engine.descriptor.ContainerMatrixEngineDescriptor;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.discovery.EngineDiscoveryRequestResolver;

public class ContainerMatrixTestsDiscoverySelectorResolver {

    // @formatter:off
    private static final EngineDiscoveryRequestResolver<ContainerMatrixTestsDescriptor> resolver = EngineDiscoveryRequestResolver.<ContainerMatrixTestsDescriptor>builder()
            .addClassContainerSelectorResolver(new IsContainerMatrixTestClass())
            .addSelectorResolver(context -> new ContainerMatrixClassSelectorResolver(context.getClassNameFilter(), context.getEngineDescriptor().getConfiguration()))
            .addSelectorResolver(context -> new ContainerMatrixMethodSelectorResolver(context.getEngineDescriptor().getConfiguration()))
            .addTestDescriptorVisitor(context -> new MethodOrderingVisitor(context.getEngineDescriptor().getConfiguration()))
            .addTestDescriptorVisitor(context -> TestDescriptor::prune)
            .build();
    // @formatter:on

    public void resolveSelectors(EngineDiscoveryRequest request, ContainerMatrixEngineDescriptor engineDescriptor) {
        // ToDo: find all Annotations for VM Lifecycle
        // for every combination, resolve
        // add to engineDescriptor
        resolver.resolve(request, engineDescriptor);
        engineDescriptor.addChild();
    }
}
