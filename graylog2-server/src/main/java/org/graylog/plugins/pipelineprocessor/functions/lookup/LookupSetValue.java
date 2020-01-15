/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.functions.lookup;

import com.google.inject.Inject;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog2.lookup.LookupTableService;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class LookupSetValue extends AbstractFunction<Object> {

    public static final String NAME = "lookup_set_value";

    private final ParameterDescriptor<String, LookupTableService.Function> lookupTableParam;
    private final ParameterDescriptor<Object, Object> keyParam;
    private final ParameterDescriptor<Object, Object> valueParam;

    @Inject
    public LookupSetValue(LookupTableService lookupTableService) {
        lookupTableParam = string("lookup_table", LookupTableService.Function.class)
                .description("The existing lookup table to use to set the given value")
                .transform(tableName -> lookupTableService.newBuilder().lookupTable(tableName).build())
                .build();
        keyParam = object("key")
                .description("The key to set in the lookup table")
                .build();
        valueParam = object("value")
                .description("The single value that should be set into the lookup table")
                .build();
    }

    @Override
    public Object evaluate(FunctionArgs args, EvaluationContext context) {
        Object key = keyParam.required(args, context);
        if (key == null) {
            return null;
        }
        LookupTableService.Function table = lookupTableParam.required(args, context);
        if (table == null) {
            return null;
        }
        Object value = valueParam.required(args, context);
        if (value == null) {
            return null;
        }
        return table.setValue(key, value).singleValue();
    }

    @Override
    public FunctionDescriptor<Object> descriptor() {
        return FunctionDescriptor.builder()
                .name(NAME)
                .description("Set a single value in the named lookup table. Returns the new value on success, null on failure.")
                .params(lookupTableParam, keyParam, valueParam)
                .returnType(Object.class)
                .build();
    }
}
