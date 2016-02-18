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
package org.graylog.plugins.pipelineprocessor.functions.messages;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import java.util.Map;

import static com.google.common.collect.ImmutableList.of;

public class SetFields extends AbstractFunction<Void> {

    public static final String NAME = "set_fields";
    public static final String FIELDS = "fields";
    private final ParameterDescriptor<Map, Map> fieldsParam;

    public SetFields() {
        fieldsParam = ParameterDescriptor.type(FIELDS, Map.class).build();
    }

    @Override
    public Void evaluate(FunctionArgs args, EvaluationContext context) {
        //noinspection unchecked
        final Map<String, Object> fields = fieldsParam.required(args, context);
        context.currentMessage().addFields(fields);
        return null;
    }

    @Override
    public FunctionDescriptor<Void> descriptor() {
        return FunctionDescriptor.<Void>builder()
                .name(NAME)
                .returnType(Void.class)
                .params(of(
                        fieldsParam
                ))
                .build();
    }

}
