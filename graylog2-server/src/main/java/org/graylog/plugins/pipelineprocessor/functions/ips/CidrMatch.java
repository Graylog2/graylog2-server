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
import org.graylog2.utilities.IpSubnet;

import java.net.UnknownHostException;

import static com.google.common.collect.ImmutableList.of;

public class CidrMatch extends AbstractFunction<Boolean> {

    public static final String NAME = "cidr_match";
    public static final String IP = "ip";

    private final ParameterDescriptor<String, IpSubnet> cidrParam;
    private final ParameterDescriptor<IpAddress, IpAddress> ipParam;

    public CidrMatch() {
        // a little ugly because newCIDR throws a checked exception :(
        cidrParam = ParameterDescriptor.string("cidr", IpSubnet.class).transform(cidrString -> {
            try {
                return new IpSubnet(cidrString);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException(e);
            }
        }).description("The CIDR subnet mask").build();
        ipParam = ParameterDescriptor.type(IP, IpAddress.class).description("The parsed IP address to match against the CIDR mask").build();
    }

    @Override
    public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
        final IpSubnet cidr = cidrParam.required(args, context);
        final IpAddress ipAddress = ipParam.required(args, context);
        if (cidr == null || ipAddress == null) {
            return null;
        }
        return cidr.contains(ipAddress.inetAddress());
    }

    @Override
    public FunctionDescriptor<Boolean> descriptor() {
        return FunctionDescriptor.<Boolean>builder()
                .name(NAME)
                .returnType(Boolean.class)
                .params(of(
                        cidrParam,
                        ipParam))
                .description("Checks if an IP address matches a CIDR subnet mask")
                .build();
    }
}
