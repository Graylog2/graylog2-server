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
package org.graylog.plugins.pipelineprocessor.functions;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.inputs.InputRegistry;

import javax.inject.Inject;

import static com.google.common.collect.ImmutableList.of;

public class InputFunction implements Function<MessageInput> {

    public static final String NAME = "input";

    private final InputRegistry inputRegistry;

    @Inject
    public InputFunction(InputRegistry inputRegistry) {
        this.inputRegistry = inputRegistry;
    }

    @Override
    public MessageInput evaluate(FunctionArgs args, EvaluationContext context) {
        final String id = args.evaluated("id", context, String.class).orElse("");
        final IOState<MessageInput> inputState = inputRegistry.getInputState(id);
        return inputState.getStoppable();
    }

    @Override
    public FunctionDescriptor<MessageInput> descriptor() {
        return FunctionDescriptor.<MessageInput>builder()
                .name(NAME)
                .returnType(MessageInput.class)
                .params(of(ParameterDescriptor.string("id")))
                .build();
    }
}
