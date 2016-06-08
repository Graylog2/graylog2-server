package org.graylog2.bindings;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.decorators.UpperCaseDecorator;
import org.graylog2.plugin.decorators.MessageDecorator;
import org.graylog2.plugin.decorators.SearchResponseDecorator;

public class DecoratorBindings extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<MessageDecorator> messageDecoratorMultibinder = Multibinder.newSetBinder(binder(), MessageDecorator.class);
        messageDecoratorMultibinder.addBinding().to(UpperCaseDecorator.class);

        Multibinder<SearchResponseDecorator> searchResponseDecoratorMultibinder = Multibinder.newSetBinder(binder(), SearchResponseDecorator.class);
    }
}
