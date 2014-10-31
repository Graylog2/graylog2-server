package org.graylog2.benchmarks.pipeline;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class ClassicModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new FactoryModuleBuilder().build(ClassicPipeline.Factory.class));
        install(new FactoryModuleBuilder().build(MessageProducer.Factory.class));

        install(new FactoryModuleBuilder().build(FilterHandler.Factory.class));
        install(new FactoryModuleBuilder().build(OutputHandler.Factory.class));

        install(new FactoryModuleBuilder().build(InputBuffer.Factory.class));
        install(new FactoryModuleBuilder().build(OutputBuffer.Factory.class));

        bind(MetricRegistry.class).asEagerSingleton();
        bind(MessageOutput.class).asEagerSingleton();
    }
}
