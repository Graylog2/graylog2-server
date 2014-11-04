package org.graylog2.benchmarks.pipeline.singlebuffer;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class SingleBufferModule extends AbstractModule {
    @Override
    protected void configure() {

        install(new FactoryModuleBuilder().build(MessageBuffer.Factory.class));
        install(new FactoryModuleBuilder().build(FilterWorker.Factory.class));
        install(new FactoryModuleBuilder().build(OutputWorker.Factory.class));
        install(new FactoryModuleBuilder().build(MessageProducer.Factory.class));
        install(new FactoryModuleBuilder().build(SingleBufferPipeline.Factory.class));

        bind(MetricRegistry.class).asEagerSingleton();
        bind(MessageOutput.class).asEagerSingleton();
    }
}
