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
package org.graylog.plugins.threatintel.whois.ip;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.threatintel.functions.misc.LookupTableFunction;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.plugin.lookup.LookupResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.util.Map;

public class WhoisLookupIpFunction extends LookupTableFunction<WhoisIpLookupResult> {

    private static final Logger LOG = LoggerFactory.getLogger(WhoisLookupIpFunction.class);

    public static final String NAME = "whois_lookup_ip";
    private static final String VALUE = "ip_address";
    private static final String PREFIX = "prefix";

    private static final String LOOKUP_TABLE_NAME = "whois";

    private final ParameterDescriptor<String, String> valueParam = ParameterDescriptor.string(VALUE).description("The IPv4 or IPv6 address to look up.").build();
    private final ParameterDescriptor<String, String> prefixParam = ParameterDescriptor.string(PREFIX).description("A prefix for results. For example \"src_addr\" will result in fields called \"src_addr_whois_org\".").build();

    private final LookupTableService.Function lookupFunction;

    @Inject
    public WhoisLookupIpFunction(final LookupTableService lookupTableService) {
        this.lookupFunction = lookupTableService.newBuilder().lookupTable(LOOKUP_TABLE_NAME).build();
    }

    @Override
    public WhoisIpLookupResult evaluate(FunctionArgs args, EvaluationContext context) {
        final String ip = valueParam.required(args, context);
        final String prefix = prefixParam.required(args, context);

        if (ip == null) {
            LOG.error("NULL parameter passed to WHOIS IP lookup.");
            return null;
        }

        if (prefix == null) {
            LOG.error("NULL prefix parameter passed to global IP lookup.");
            return null;
        }

        LOG.debug("Running WHOIS lookup for IP [{}] with prefix [{}].", ip, prefix);

        final LookupResult lookupResult = this.lookupFunction.lookup(ip);

        if (lookupResult == null || lookupResult.isEmpty()) {
            return null;
        }

        final Map<Object, Object> fields = lookupResult.multiValue();

        final WhoisIpLookupResult result = new WhoisIpLookupResult(
                String.valueOf(fields.get(WhoisDataAdapter.ORGANIZATION_FIELD)),
                String.valueOf(fields.get(WhoisDataAdapter.COUNTRY_CODE_FIELD))
        );
        result.setPrefix(prefix.trim());
        return result;
    }

    @Override
    public FunctionDescriptor<WhoisIpLookupResult> descriptor() {
        return FunctionDescriptor.<WhoisIpLookupResult>builder()
                .name(NAME)
                .description("Get WHOIS information of an IP address")
                .params(valueParam, prefixParam)
                .returnType(WhoisIpLookupResult.class)
                .build();
    }


}
