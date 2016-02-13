/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor;

import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.functions.BooleanCoercion;
import org.graylog.plugins.pipelineprocessor.functions.DoubleCoercion;
import org.graylog.plugins.pipelineprocessor.functions.FromInput;
import org.graylog.plugins.pipelineprocessor.functions.LongCoercion;
import org.graylog.plugins.pipelineprocessor.functions.strings.Abbreviate;
import org.graylog.plugins.pipelineprocessor.functions.strings.Capitalize;
import org.graylog.plugins.pipelineprocessor.functions.strings.Lowercase;
import org.graylog.plugins.pipelineprocessor.functions.strings.RegexMatch;
import org.graylog.plugins.pipelineprocessor.functions.StringCoercion;
import org.graylog.plugins.pipelineprocessor.functions.messages.CreateMessage;
import org.graylog.plugins.pipelineprocessor.functions.messages.DropMessage;
import org.graylog.plugins.pipelineprocessor.functions.messages.HasField;
import org.graylog.plugins.pipelineprocessor.functions.messages.RemoveField;
import org.graylog.plugins.pipelineprocessor.functions.messages.RouteToStream;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetField;
import org.graylog.plugins.pipelineprocessor.functions.strings.SwapCase;
import org.graylog.plugins.pipelineprocessor.functions.strings.Uncapitalize;
import org.graylog.plugins.pipelineprocessor.functions.strings.Uppercase;
import org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter;
import org.graylog.plugins.pipelineprocessor.rest.PipelineResource;
import org.graylog.plugins.pipelineprocessor.rest.PipelineStreamResource;
import org.graylog.plugins.pipelineprocessor.rest.RuleResource;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.PluginModule;

import java.util.Collections;
import java.util.Set;

public class PipelineProcessorModule extends PluginModule {

    @Override
    public Set<? extends PluginConfigBean> getConfigBeans() {
        return Collections.emptySet();
    }

    @Override
    protected void configure() {
        //addMessageProcessor(NaiveRuleProcessor.class);
        addMessageProcessor(PipelineInterpreter.class);
        addRestResource(RuleResource.class);
        addRestResource(PipelineResource.class);
        addRestResource(PipelineStreamResource.class);

        // built-in functions
        addMessageProcessorFunction(BooleanCoercion.NAME, BooleanCoercion.class);
        addMessageProcessorFunction(DoubleCoercion.NAME, DoubleCoercion.class);
        addMessageProcessorFunction(LongCoercion.NAME, LongCoercion.class);
        addMessageProcessorFunction(StringCoercion.NAME, StringCoercion.class);

        // message related functions
        addMessageProcessorFunction(HasField.NAME, HasField.class);
        addMessageProcessorFunction(SetField.NAME, SetField.class);
        addMessageProcessorFunction(RemoveField.NAME, RemoveField.class);

        addMessageProcessorFunction(DropMessage.NAME, DropMessage.class);
        addMessageProcessorFunction(CreateMessage.NAME, CreateMessage.class);
        addMessageProcessorFunction(RouteToStream.NAME, RouteToStream.class);

        // input related functions
        addMessageProcessorFunction(FromInput.NAME, FromInput.class);

        // generic functions
        addMessageProcessorFunction(RegexMatch.NAME, RegexMatch.class);

        // string functions
        addMessageProcessorFunction(Abbreviate.NAME, Abbreviate.class);
        addMessageProcessorFunction(Capitalize.NAME, Capitalize.class);
        addMessageProcessorFunction(Lowercase.NAME, Lowercase.class);
        addMessageProcessorFunction(SwapCase.NAME, SwapCase.class);
        addMessageProcessorFunction(Uncapitalize.NAME, Uncapitalize.class);
        addMessageProcessorFunction(Uppercase.NAME, Uppercase.class);


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
