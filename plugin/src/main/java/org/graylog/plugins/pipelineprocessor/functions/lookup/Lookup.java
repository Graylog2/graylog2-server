package org.graylog.plugins.pipelineprocessor.functions.lookup;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.plugin.lookup.LookupResult;

import java.util.Collections;
import java.util.Map;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;
import static org.graylog2.plugin.lookup.LookupResult.SINGLE_VALUE_KEY;

public class Lookup extends AbstractFunction<Map<Object, Object>> {

    public static final String NAME = "lookup";

    private final ParameterDescriptor<String, LookupTableService.Function> lookupTableParam;
    private final ParameterDescriptor<Object, Object> keyParam;
    private final ParameterDescriptor<Object, Object> defaultParam;

    @Inject
    public Lookup(LookupTableService lookupTableService) {
        lookupTableParam = string("lookup_table", LookupTableService.Function.class)
                .transform(tableName -> lookupTableService.newBuilder().lookupTable(tableName).build())
                .build();
        keyParam = object("key").build();
        defaultParam = object("default").optional().build();
    }

    @Override
    public Map<Object, Object> evaluate(FunctionArgs args, EvaluationContext context) {
        Object key = keyParam.required(args, context);
        if (key == null) {
            return Collections.singletonMap(SINGLE_VALUE_KEY, defaultParam.optional(args, context));
        }
        LookupTableService.Function table = lookupTableParam.required(args, context);
        if (table == null) {
            return Collections.singletonMap(SINGLE_VALUE_KEY, defaultParam.optional(args, context));
        }
        LookupResult result = table.lookup(key);
        if (result == null || result.isEmpty()) {
            return Collections.singletonMap(SINGLE_VALUE_KEY, defaultParam.optional(args, context));
        }
        return result.multiValue();
    }

    @Override
    public FunctionDescriptor<Map<Object, Object>> descriptor() {
        //noinspection unchecked
        return FunctionDescriptor.<Map<Object, Object>>builder()
                .name(NAME)
                .description("Looks a value up in the named lookup table.")
                .params(lookupTableParam, keyParam)
                .returnType((Class<? extends Map<Object, Object>>) new TypeLiteral<Map<Object, Object>>() {}.getRawType())
                .build();
    }
}
