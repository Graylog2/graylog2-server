/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.pipelineprocessor.functions.conversion;

import com.google.inject.TypeLiteral;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class HexToDecimalConversion extends AbstractFunction<List<Long>> {

    public static final String NAME = "hex_to_decimal_byte_list";

    private static final String VALUE = "value";
    private final ParameterDescriptor<String, String> valueParam;

    public HexToDecimalConversion() {
        valueParam = string(VALUE).ruleBuilderVariable().description("Hex string to convert").build();
    }

    @Override
    public List<Long> evaluate(FunctionArgs args, EvaluationContext context) {
        final Object evaluated = valueParam.required(args, context);

        if (evaluated != null) {
            String hexString = String.valueOf(evaluated);

            // Remove "0x" prefix if present
            if (hexString.startsWith("0x") || hexString.startsWith("0X")) {
                hexString = hexString.substring(2);
            }

            // Ensure even number of characters for correct byte parsing
            if (hexString.length() % 2 != 0) {
                hexString = "0" + hexString;
            }

            final List<Long> byteValues = new ArrayList<>();

            try {
                for (int i = 0; i < hexString.length(); i += 2) {
                    final String byteHex = hexString.substring(i, i + 2);
                    byteValues.add((long) Integer.parseInt(byteHex, 16));
                }
                return byteValues;
            } catch (NumberFormatException e) {
                log.error("Invalid hex string [{}]", hexString, e);
            }
        }

        return null;
    }

    @Override
    public FunctionDescriptor<List<Long>> descriptor() {
        //noinspection unchecked
        return FunctionDescriptor.<List<Long>>builder()
                .name(NAME)
                .returnType((Class<? extends List<Long>>) new TypeLiteral<List<Long>>() {}.getRawType())
                .params(of(valueParam))
                .description("Converts a hexadecimal string to an array of decimal (base 10) byte values.")
                .ruleBuilderEnabled()
                .ruleBuilderName("Convert to decimal array")
                .ruleBuilderTitle("Convert '${value}' to decimal array")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.CONVERSION)
                .build();
    }
}

