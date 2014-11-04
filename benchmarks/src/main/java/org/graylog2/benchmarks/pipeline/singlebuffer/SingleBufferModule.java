package org.graylog2.benchmarks.pipeline.singlebuffer;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.graylog2.benchmarks.pipeline.classic.MessageProducer;
import org.graylog2.benchmarks.pipeline.classic.MessageOutput;

public class SingleBufferModule extends AbstractModule {
    @Override
    protected void configure() {

        install(new FactoryModuleBuilder().build(MessageProducer.Factory.class));

        bind(MetricRegistry.class).asEagerSingleton();
        bind(MessageOutput.class).asEagerSingleton();
    }
}
