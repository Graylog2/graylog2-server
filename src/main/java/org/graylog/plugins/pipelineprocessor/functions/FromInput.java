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
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.inputs.InputRegistry;

import javax.inject.Inject;

import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class FromInput extends AbstractFunction<Boolean> {

    public static final String NAME = "from_input";
    public static final String ID_ARG = "id";
    public static final String NAME_ARG = "name";

    private final InputRegistry inputRegistry;

    @Inject
    public FromInput(InputRegistry inputRegistry) {
        this.inputRegistry = inputRegistry;
    }

    @Override
    public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
        String id = args.evaluated(ID_ARG, context, String.class).orElse("");

        MessageInput input = null;
        if ("".equals(id)) {
            final String name = args.evaluated(NAME_ARG, context, String.class).orElse("");
            for (IOState<MessageInput> messageInputIOState : inputRegistry.getInputStates()) {
                final MessageInput messageInput = messageInputIOState.getStoppable();
                if (messageInput.getTitle().equalsIgnoreCase(name)) {
                    input = messageInput;
                    break;
                }
            }
            if ("".equals(name)) {
                return null;
            }
        } else {
            final IOState<MessageInput> inputState = inputRegistry.getInputState(id);
            if (inputState != null) {
                input = inputState.getStoppable();
            }

        }
        return input != null
                && context.currentMessage().getSourceInputId().equals(input.getId());
    }

    @Override
    public FunctionDescriptor<Boolean> descriptor() {
        return FunctionDescriptor.<Boolean>builder()
                .name(NAME)
                .returnType(Boolean.class)
                .params(of(
                        string(ID_ARG).optional().build(),
                        string(NAME_ARG).optional().build()))
                .build();
    }
}
