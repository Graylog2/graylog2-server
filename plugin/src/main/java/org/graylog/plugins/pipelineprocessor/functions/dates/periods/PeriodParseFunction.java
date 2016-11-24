package org.graylog.plugins.pipelineprocessor.functions.dates.periods;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.joda.time.Period;

public class PeriodParseFunction extends AbstractFunction<Period> {

    public static final String NAME = "period";
    private final ParameterDescriptor<String, Period> value =
            ParameterDescriptor
                    .string("value", Period.class)
                    .transform(Period::parse)
                    .build();


    @Override
    public Period evaluate(FunctionArgs args, EvaluationContext context) {
        return value.required(args, context);
    }

    @Override
    public FunctionDescriptor<Period> descriptor() {
        return FunctionDescriptor.<Period>builder()
                .name(NAME)
                .description("Parses a ISO 8601 period from the specified string.")
                .pure(true)
                .returnType(Period.class)
                .params(value)
                .build();
    }
}
