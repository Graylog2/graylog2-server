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
import org.graylog.plugins.threatintel.tools.Domain;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.plugin.lookup.LookupResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

public class AbuseChRansomDomainLookupFunction extends LookupTableFunction<GenericLookupResult> {

    private static final Logger LOG = LoggerFactory.getLogger(AbuseChRansomDomainLookupFunction.class);

    public static final String NAME = "abusech_ransom_lookup_domain";
    private static final String VALUE = "domain_name";
    private static final String LOOKUP_TABLE_NAME = "abuse-ch-ransomware-domains";

    private final ParameterDescriptor<String, String> valueParam = ParameterDescriptor.string(VALUE).description("The domain to look up. Example: foo.example.org (A trailing dot ('.') will be ignored.)").build();

    private final LookupTableService.Function lookupFunction;

    @Inject
    public AbuseChRansomDomainLookupFunction(final LookupTableService lookupTableService) {
        this.lookupFunction = lookupTableService.newBuilder().lookupTable(LOOKUP_TABLE_NAME).build();
    }

    @Override
    public GenericLookupResult evaluate(FunctionArgs args, EvaluationContext context) {
        String domain = valueParam.required(args, context);
        if (domain == null) {
            LOG.error("NULL parameter passed to abuse.ch Ransomware domain lookup.");
            return null;
        }

        domain = Domain.prepareDomain(domain);

        LOG.debug("Running abuse.ch Ransomware lookup for domain [{}].", domain);

        final LookupResult lookupResult = this.lookupFunction.lookup(domain.trim());
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
                .description("Match a domain name against the abuse.ch Ransomware Domain Blocklist. (RW_DOMBL)")
                .params(valueParam)
                .returnType(GenericLookupResult.class)
                .build();
    }

}
