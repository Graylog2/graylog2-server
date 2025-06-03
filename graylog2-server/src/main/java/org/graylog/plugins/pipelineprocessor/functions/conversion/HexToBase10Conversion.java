package org.graylog.plugins.pipelineprocessor.functions.conversion;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;

import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class HexToBase10Conversion extends AbstractFunction<String> {

    public static final String NAME = "hex_to_dec";

    private final ParameterDescriptor<String, String> hexParam;

    public HexToBase10Conversion(ParameterDescriptor<String, String> hexParam) {
        this.hexParam = string("hex").ruleBuilderVariable().description("The hexadecimal string to convert").build();
    }

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        final String hex = hexParam.required(args, context);
        if (hex == null) {
            return null;
        }
        try {
            return String.valueOf(Integer.parseInt(hex.replace("0x", ""), 16));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid hexadecimal input: " + hex, e);
        }
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        return FunctionDescriptor.<String>builder()
                .name(NAME)
                .description("Converts a hexadecimal string to its decimal (base 10) integer representation.")
                .params(of(hexParam))
                .returnType(String.class)
                .ruleBuilderEnabled()
                .ruleBuilderName("Convert to decimal")
                .ruleBuilderTitle("Convert '${hex}' to decimal")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.CONVERSION)
                .build();
    }
}

