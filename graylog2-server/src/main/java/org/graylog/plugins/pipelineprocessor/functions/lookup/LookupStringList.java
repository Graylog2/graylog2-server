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

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.plugin.lookup.LookupResult;

import java.util.List;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class LookupStringList extends AbstractFunction<List<String>> {

    public static final String NAME = "lookup_string_list";

    private final ParameterDescriptor<String, LookupTableService.Function> lookupTableParam;
    private final ParameterDescriptor<Object, Object> keyParam;
    @SuppressWarnings("rawtypes") // we cannot store class instances of generic types
    private final ParameterDescriptor<List, List> defaultParam;

    @Inject
    public LookupStringList(LookupTableService lookupTableService) {
        lookupTableParam = string("lookup_table", LookupTableService.Function.class)
                .description("The existing lookup table to use to lookup the given key")
                .transform(tableName -> lookupTableService.newBuilder().lookupTable(tableName).build())
                .build();
        keyParam = object("key")
                .description("The key to lookup in the table")
                .build();
        defaultParam = ParameterDescriptor.type("default", List.class)
                .description("The default list value that should be used if there is no lookup result")
                .optional()
                .build();
    }

    @Override
    public List<String> evaluate(FunctionArgs args, EvaluationContext context) {
        Object key = keyParam.required(args, context);
        if (key == null) {
            //noinspection unchecked
            return defaultParam.optional(args, context).orElse(ImmutableList.of());
        }
        LookupTableService.Function table = lookupTableParam.required(args, context);
        if (table == null) {
            //noinspection unchecked
            return defaultParam.optional(args, context).orElse(ImmutableList.of());
        }
        LookupResult result = table.lookup(key);
        if (result == null || result.isEmpty()) {
            //noinspection unchecked
            return defaultParam.optional(args, context).orElse(ImmutableList.of());
        }
        return result.stringListValue();
    }

    @Override
    public FunctionDescriptor<List<String>> descriptor() {
        //noinspection unchecked
        return FunctionDescriptor.<List<String>>builder()
                .name(NAME)
                .description("Looks up a string list value in the named lookup table.")
                .params(lookupTableParam, keyParam, defaultParam)
                .returnType((Class<? extends List<String>>) new TypeLiteral<List<String>>() {}.getRawType())
                .build();
    }
}
