package org.graylog.plugins.cef;

import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import org.graylog.plugins.cef.codec.CEFCodec;
import org.graylog.plugins.cef.input.CEFTCPInput;
import org.graylog.plugins.cef.input.CEFUDPInput;
import org.graylog.plugins.cef.pipelines.rules.CEFParserFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.PluginModule;

import java.util.Collections;
import java.util.Set;

public class CEFInputModule extends PluginModule {

    @Override
    public Set<? extends PluginConfigBean> getConfigBeans() {
        return Collections.emptySet();
    }

    @Override
    protected void configure() {
        // Register message input.
        addCodec(CEFCodec.NAME, CEFCodec.class);
        addMessageInput(CEFUDPInput.class);
        addMessageInput(CEFTCPInput.class);

        // Register pipeline function.
        addMessageProcessorFunction(CEFParserFunction.NAME, CEFParserFunction.class);
    }

    protected void addMessageProcessorFunction(String name, Class<? extends Function<?>> functionClass) {
        addMessageProcessorFunction(binder(), name, functionClass);
    }

    public static MapBinder<String, Function<?>> processorFunctionBinder(Binder binder) {
        return MapBinder.newMapBinder(binder, TypeLiteral.get(String.class), new TypeLiteral<Function<?>>() {});
    }

    public static void addMessageProcessorFunction(Binder binder, String name, Class<? extends Function<?>> functionClass) {
        processorFunctionBinder(binder).addBinding(name).to(functionClass);

    }

}
