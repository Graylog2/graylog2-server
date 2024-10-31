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
package org.graylog.plugins.threatintel.functions.misc;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.threatintel.tools.PrivateNet;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import static com.codahale.metrics.MetricRegistry.name;

public class PrivateNetLookupFunction extends AbstractFunction<Boolean> {

    private static final Logger LOG = LoggerFactory.getLogger(PrivateNetLookupFunction.class);

    public static final String NAME = "in_private_net";
    private static final String VALUE = "ip_address";

    private final ParameterDescriptor<String, String> valueParam = ParameterDescriptor.string(VALUE).description("The IP address to look up.").build();

    protected Timer lookupTime;

    @Inject
    public PrivateNetLookupFunction(final MetricRegistry metricRegistry) {
        this.lookupTime = metricRegistry.timer(name(this.getClass(), "lookupTime"));
    }

    @Override
    public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
        String ip = valueParam.required(args, context);
        if (ip == null || ip.isEmpty()) {
            LOG.debug("NULL or empty parameter passed to private network lookup.");
            return null;
        }

        LOG.debug("Running private network lookup for IP [{}].", ip);

        try {
            Timer.Context timer = this.lookupTime.time();
            boolean result = PrivateNet.isInPrivateAddressSpace(ip);
            timer.stop();

            return result;
        } catch (Exception e) {
            LOG.error("Could not run private net lookup for IP [{}]: {}", ip, ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }


    @Override
    public FunctionDescriptor<Boolean> descriptor() {
        return FunctionDescriptor.<Boolean>builder()
                .name(NAME)
                .description("Check if an IP address is in a private network as defined in RFC 1918 (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16) or RFC 4193 (fc00::/7)")
                .params(valueParam)
                .returnType(Boolean.class)
                .build();
    }

}
