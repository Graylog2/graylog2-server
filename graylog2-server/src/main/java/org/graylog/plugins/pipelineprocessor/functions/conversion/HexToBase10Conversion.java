package org.graylog.plugins.pipelineprocessor.functions.conversion;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.integer;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;

public class HexToBase10Conversion extends AbstractFunction<Long> {

    public static final String NAME = "hex_to_double";

    private static final String VALUE = "value";
    private static final String DEFAULT = "default";
    private final ParameterDescriptor<Object, Object> valueParam;
    private final ParameterDescriptor<Long, Long> defaultParam;

    public HexToBase10Conversion() {
        valueParam = object(VALUE).ruleBuilderVariable().description("Value to convert").build();
        defaultParam = integer(DEFAULT).optional().allowNegatives(false).description("Used when 'value' is null, defaults to 0").build();
    }

    @Override
    public Long evaluate(FunctionArgs args, EvaluationContext context) {
        final Object evaluated = valueParam.required(args, context);
        final Long defaultValue = defaultParam.optional(args, context).orElse(0L);

//        if (evaluated != null) {
//            final String s = String.valueOf(evaluated);
//            try {
//                final byte[] hexBytes = HexFormat.of().parseHex(s);
//                return IntStream.range(0, hexBytes.length).asLongStream().boxed().toList();
//            } catch (NumberFormatException ignored) {
//            }
//        }

        return defaultValue;
    }

    @Override
    public FunctionDescriptor<Long> descriptor() {
        return FunctionDescriptor.<Long>builder()
                .name(NAME)
                .returnType(Long.class)
                .params(of(
                        valueParam,
                        defaultParam
                ))
                .description("Converts a hexadecimal string to its decimal (base 10) representation.")
                .ruleBuilderEnabled()
                .ruleBuilderName("Convert to decimal")
                .ruleBuilderTitle("Convert '${value}' to decimal")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.CONVERSION)
                .build();
    }
}

