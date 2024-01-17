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
package org.graylog.plugins.threatintel.functions.abusech;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.threatintel.functions.GenericLookupResult;
import org.graylog.plugins.threatintel.functions.misc.LookupTableFunction;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.plugin.lookup.LookupResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

public class AbuseChRansomIpLookupFunction extends LookupTableFunction<GenericLookupResult> {

    private static final Logger LOG = LoggerFactory.getLogger(AbuseChRansomIpLookupFunction.class);

    public static final String NAME = "abusech_ransom_lookup_ip";
    private static final String VALUE = "ip_address";
    private static final String LOOKUP_TABLE_NAME = "abuse-ch-ransomware-ip";

    private final ParameterDescriptor<String, String> valueParam = ParameterDescriptor.string(VALUE).description("The IPv4 or IPv6 address to look up. Example: 198.51.100.1 or 2001:0db8:85a3:0000:0000:8a2e:0370:7334").build();

    private final LookupTableService.Function lookupFunction;

    @Inject
    public AbuseChRansomIpLookupFunction(final LookupTableService lookupTableService) {
        this.lookupFunction = lookupTableService.newBuilder().lookupTable(LOOKUP_TABLE_NAME).build();
    }

    @Override
    public GenericLookupResult evaluate(FunctionArgs args, EvaluationContext context) {
        String ip = valueParam.required(args, context);
        if (ip == null) {
            LOG.error("NULL parameter passed to abuse.ch Ransomware IP lookup.");
            return null;
        }

        LOG.debug("Running abuse.ch Ransomware lookup for IP [{}].", ip);

        final LookupResult lookupResult = this.lookupFunction.lookup(ip.trim());
        if (lookupResult != null && !lookupResult.isEmpty() && lookupResult.singleValue() != null) {
            if (lookupResult.singleValue() instanceof Boolean) {
                return (Boolean) lookupResult.singleValue() ? GenericLookupResult.TRUE : GenericLookupResult.FALSE;
            }
            if (lookupResult.singleValue() instanceof String) {
                return Boolean.valueOf((String) lookupResult.singleValue()) ? GenericLookupResult.TRUE : GenericLookupResult.FALSE;
            }
        }

        return GenericLookupResult.FALSE;
    }

    @Override
    public FunctionDescriptor<GenericLookupResult> descriptor() {
        return FunctionDescriptor.<GenericLookupResult>builder()
                .name(NAME)
                .description("Match a IPv4 or IPv6 address against the abuse.ch Ransomware IP Blocklist. (RW_IPBL)")
                .params(valueParam)
                .returnType(GenericLookupResult.class)
                .build();
    }

}
