package org.graylog2.bindings;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.plugin.dashboards.widgets.WidgetStrategy;

public class WidgetStrategyBindings extends AbstractModule {
    @Override
    protected void configure() {
        final TypeLiteral<Class<? extends WidgetStrategy>> type = new TypeLiteral<Class<? extends WidgetStrategy>>(){};
        final Multibinder<Class<? extends WidgetStrategy>> widgetStrategyBinder = Multibinder.newSetBinder(binder(), type);
    }
}
