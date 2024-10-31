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
package org.graylog.plugins.threatintel.functions.global;

import com.google.common.eventbus.EventBus;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.threatintel.ThreatIntelPluginConfiguration;
import org.graylog.plugins.threatintel.functions.DomainFunctions;
import org.graylog.plugins.threatintel.functions.GenericLookupResult;
import org.graylog.plugins.threatintel.functions.abusech.AbuseChRansomDomainLookupFunction;
import org.graylog.plugins.threatintel.functions.misc.LookupTableFunction;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.util.Map;

public class GlobalDomainLookupFunction extends AbstractGlobalLookupFunction {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalDomainLookupFunction.class);

    public static final String NAME = "threat_intel_lookup_domain";
    private static final String VALUE = "domain_name";
    private static final String PREFIX = "prefix";

    private final ParameterDescriptor<String, String> valueParam = ParameterDescriptor.string(VALUE).description("The domain to look up. Example: foo.example.org (A trailing dot ('.') will be ignored.)").build();
    private final ParameterDescriptor<String, String> prefixParam = ParameterDescriptor.string(PREFIX).description("A prefix for results. For example \"src\" will result in fields called \"src_threat_indicated\".").build();

    private Map<String, LookupTableFunction<? extends GenericLookupResult>> domainFunctions;

    @Inject
    public GlobalDomainLookupFunction(@DomainFunctions final Map<String, LookupTableFunction<? extends GenericLookupResult>> domainFunctions,
                                      final ClusterConfigService clusterConfigService,
                                      final EventBus serverEventBus) {
        super(clusterConfigService, serverEventBus);
        this.domainFunctions = domainFunctions;
    }

    @Override
    public GlobalLookupResult evaluate(FunctionArgs args, EvaluationContext context) {
        final String domain = valueParam.required(args, context);
        final String prefix = prefixParam.required(args, context);

        if (domain == null) {
            LOG.error("NULL value parameter passed to global domain lookup.");
            return null;
        }

        if (prefix == null) {
            LOG.error("NULL prefix parameter passed to global domain lookup.");
            return null;
        }

        LOG.debug("Running global lookup for domain [{}] with prefix [{}].", domain, prefix);

        return matchEntityAgainstFunctions(this.domainFunctions, args, context, prefix);
    }

    @Override
    boolean isEnabled(LookupTableFunction<? extends GenericLookupResult> function) {
        final ThreatIntelPluginConfiguration configuration = this.threatIntelPluginConfiguration();
        if (function.getClass().equals(AbuseChRansomDomainLookupFunction.class)) {
            return configuration.abusechRansomEnabled();
        }
        return true;
    }

    @Override
    public FunctionDescriptor<GlobalLookupResult> descriptor() {
        return FunctionDescriptor.<GlobalLookupResult>builder()
                .name(NAME)
                .description("Match a domain name against all enabled threat intel sources. (except OTX)")
                .params(valueParam, prefixParam)
                .returnType(GlobalLookupResult.class)
                .build();
    }

}
