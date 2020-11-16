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

import static com.google.common.collect.ImmutableList.of;

import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.util.IllegalFormatException;
import java.util.Optional;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

public class IpAddressConversion extends AbstractFunction<IpAddress> {

    public static final String NAME = "to_ip";
    private static final InetAddress ANYV4 = InetAddresses.forString("0.0.0.0");

    private final ParameterDescriptor<Object, Object> ipParam;
    private final ParameterDescriptor<String, String> defaultParam;

    public IpAddressConversion() {
        ipParam = ParameterDescriptor.object("ip").description("Value to convert").build();
        defaultParam = ParameterDescriptor.string("default").optional().description("Used when 'ip' is null or malformed, defaults to '0.0.0.0'").build();
    }

    @Override
    public IpAddress evaluate(FunctionArgs args, EvaluationContext context) {
        final Object ip = ipParam.required(args, context);
        try {
            if (ip instanceof Number) {
                // this is only valid for IPv4 addresses, v6 requires 128 bits which we don't support
                return new IpAddress(InetAddresses.fromInteger(((Number) ip).intValue()));
            } else {
                return new IpAddress(InetAddresses.forString(String.valueOf(ip)));
            }
        } catch (IllegalArgumentException e) {
            final Optional<String> defaultValue = defaultParam.optional(args, context);
            if (!defaultValue.isPresent()) {
                return new IpAddress(ANYV4);
            }
            try {
                return new IpAddress(InetAddresses.forString(defaultValue.get()));
            } catch (IllegalFormatException e1) {
                log.warn("Parameter `default` for to_ip() is not a valid IP address: {}", defaultValue.get());
                throw e1;
            }
        }
    }

    @Override
    public FunctionDescriptor<IpAddress> descriptor() {
        return FunctionDescriptor.<IpAddress>builder()
                .name(NAME)
                .returnType(IpAddress.class)
                .params(of(
                        ipParam,
                        defaultParam
                ))
                .description("Converts a value to an IPAddress using its string representation")
                .build();
    }
}
