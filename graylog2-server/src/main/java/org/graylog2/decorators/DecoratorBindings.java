package org.graylog2.decorators;

import com.google.inject.multibindings.Multibinder;
import org.graylog2.plugin.decorators.MessageDecorator;
import org.graylog2.plugin.decorators.SearchResponseDecorator;
import org.graylog2.plugin.inject.Graylog2Module;

public class DecoratorBindings extends Graylog2Module {
    @Override
    protected void configure() {
        Multibinder<MessageDecorator> messageDecoratorMultibinder = Multibinder.newSetBinder(binder(), MessageDecorator.class);
        //messageDecoratorMultibinder.addBinding().to(UpperCaseDecorator.class);

        Multibinder<SearchResponseDecorator> searchResponseDecoratorMultibinder = Multibinder.newSetBinder(binder(), SearchResponseDecorator.class);
    }
}
