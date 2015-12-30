package org.graylog.plugins.messageprocessor;

import com.google.inject.Binder;
import com.google.inject.multibindings.MapBinder;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.ast.functions.builtin.InputFunction;
import org.graylog.plugins.messageprocessor.processors.NaiveRuleProcessor;
import org.graylog.plugins.messageprocessor.rest.MessageProcessorRuleResource;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.PluginModule;

import java.util.Collections;
import java.util.Set;

public class ProcessorPluginModule extends PluginModule {

    @Override
    public Set<? extends PluginConfigBean> getConfigBeans() {
        return Collections.emptySet();
    }

    @Override
    protected void configure() {
        addMessageProcessor(NaiveRuleProcessor.class);
        addRestResource(MessageProcessorRuleResource.class);

        // built-in functions
        addMessageProcessorFunction(InputFunction.NAME, InputFunction.class);
    }

    protected void addMessageProcessorFunction(String name, Class<? extends Function> functionClass) {
        addMessageProcessorFunction(binder(), name, functionClass);
    }

    public static MapBinder<String, Function> processorFunctionBinder(Binder binder) {
        return MapBinder.newMapBinder(binder, String.class, Function.class);
    }

    public static void addMessageProcessorFunction(Binder binder, String name, Class<? extends Function> functionClass) {
        processorFunctionBinder(binder).addBinding(name).to(functionClass);

    }
}
