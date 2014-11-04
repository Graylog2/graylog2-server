package org.graylog2.benchmarks.pipeline.classicpooled;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class ClassicPooledModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new FactoryModuleBuilder().build(ClassicPooledPipeline.Factory.class));
        install(new FactoryModuleBuilder().build(MessageProducer.Factory.class));

        install(new FactoryModuleBuilder().build(FilterWorker.Factory.class));
        install(new FactoryModuleBuilder().build(OutputHandler.Factory.class));

        install(new FactoryModuleBuilder().build(WorkerPoolInputBuffer.Factory.class));
        install(new FactoryModuleBuilder().build(OutputBuffer.Factory.class));

        bind(MetricRegistry.class).asEagerSingleton();
        bind(MessageOutput.class).asEagerSingleton();
    }
}
