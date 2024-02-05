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
package org.graylog.plugins.pipelineprocessor.functions.ips;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;

import static com.google.common.collect.ImmutableList.of;

public class IpAnonymize extends AbstractFunction<IpAddress> {

    public static final String NAME = "anonymize_ip";

    private final ParameterDescriptor<Object, IpAddress> ipParam;

    public IpAnonymize() {
        ipParam = ParameterDescriptor.object("ip", IpAddress.class).ruleBuilderVariable().description("Value to convert").build();
    }

    @Override
    public IpAddress evaluate(FunctionArgs args, EvaluationContext context) {
        final IpAddress ip = ipParam.required(args, context);
        return ip.getAnonymized();
    }

    @Override
    public FunctionDescriptor<IpAddress> descriptor() {
        return FunctionDescriptor.<IpAddress>builder()
                .name(NAME)
                .returnType(IpAddress.class)
                .params(of(
                        ipParam
                ))
                .description("Anonymize an IPAddress by setting the last octet to 0")
                .ruleBuilderEnabled()
                .ruleBuilderName("Anonymize IP")
                .ruleBuilderTitle("Anonymize IP '${ip}'")
                .ruleBuilderFunctionGroup(RuleBuilderFunctionGroup.OTHER)
                .build();
    }
}
