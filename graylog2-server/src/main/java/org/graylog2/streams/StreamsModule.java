package org.graylog2.streams;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.streams.input.StreamRuleInputsProvider;
import org.graylog2.streams.input.StreamRuleServerInputsProvider;

public class StreamsModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(StreamRuleService.class).to(StreamRuleServiceImpl.class);
        bind(StreamService.class).to(StreamServiceImpl.class);

        Multibinder<StreamRuleInputsProvider> uriBinder = Multibinder.newSetBinder(binder(), StreamRuleInputsProvider.class);
        uriBinder.addBinding().to(StreamRuleServerInputsProvider.class);
    }
}
