package org.graylog.plugins.pipelineprocessor.functions.strings;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import static com.google.common.primitives.Ints.saturatedCast;

public class Abbreviate extends AbstractFunction<String> {

    public static final String NAME = "abbreviate";
    private static final String VALUE = "value";
    private static final String WIDTH = "width";

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = args.param(VALUE).evalRequired(args, context, String.class);
        final Long maxWidth = args.param(WIDTH).evalRequired(args, context, Long.class);

        return StringUtils.abbreviate(value, saturatedCast(maxWidth));
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        ImmutableList.Builder<ParameterDescriptor> params = ImmutableList.builder();
        params.add();

        return FunctionDescriptor.<String>builder()
                .name(NAME)
                .returnType(String.class)
                .params(ImmutableList.of(
                        ParameterDescriptor.string(VALUE).build(),
                        ParameterDescriptor.string(WIDTH).build()
                ))
                .build();
    }
}
