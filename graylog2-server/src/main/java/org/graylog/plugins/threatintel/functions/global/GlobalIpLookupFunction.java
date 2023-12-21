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
import org.graylog.plugins.threatintel.functions.GenericLookupResult;
import org.graylog.plugins.threatintel.functions.IPFunctions;
import org.graylog.plugins.threatintel.functions.abusech.AbuseChRansomIpLookupFunction;
import org.graylog.plugins.threatintel.functions.misc.LookupTableFunction;
import org.graylog.plugins.threatintel.functions.otx.OTXIPLookupFunction;
import org.graylog.plugins.threatintel.functions.spamhaus.SpamhausIpLookupFunction;
import org.graylog.plugins.threatintel.functions.tor.TorExitNodeLookupFunction;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.util.Map;

public class GlobalIpLookupFunction extends AbstractGlobalLookupFunction {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalIpLookupFunction.class);

    public static final String NAME = "threat_intel_lookup_ip";
    private static final String VALUE = "ip_address";
    private static final String PREFIX = "prefix";

    private final ParameterDescriptor<String, String> valueParam = ParameterDescriptor.string(VALUE).description("The IPv4 or IPv6 address to look up.").build();
    private final ParameterDescriptor<String, String> prefixParam = ParameterDescriptor.string(PREFIX).description("A prefix for results. For example \"src_addr\" will result in fields called \"src_addr_threat_indicated\".").build();

    private Map<String, LookupTableFunction<? extends GenericLookupResult>> ipFunctions;

    @Inject
    public GlobalIpLookupFunction(@IPFunctions final Map<String, LookupTableFunction<? extends GenericLookupResult>> ipFunctions,
                                  final ClusterConfigService clusterConfigService,
                                  final EventBus serverEventBus) {
        super(clusterConfigService, serverEventBus);
        this.ipFunctions = ipFunctions;
    }

    @Override
    public GlobalLookupResult evaluate(FunctionArgs args, EvaluationContext context) {
        final String ip = valueParam.required(args, context);
        final String prefix = prefixParam.required(args, context);

        if (ip == null) {
            LOG.error("NULL value parameter passed to global IP lookup.");
            return null;
        }

        if (prefix == null) {
            LOG.error("NULL prefix parameter passed to global IP lookup.");
            return null;
        }

        LOG.debug("Running global lookup for IP [{}] with prefix [{}].", ip, prefix);

        return matchEntityAgainstFunctions(this.ipFunctions, args, context, prefix);
    }

    @Override
    boolean isEnabled(LookupTableFunction<? extends GenericLookupResult> function) {
        final ThreatIntelPluginConfiguration configuration = this.threatIntelPluginConfiguration();
        if (function.getClass().equals(TorExitNodeLookupFunction.class)) {
            return configuration.torEnabled();
        }
        if (function.getClass().equals(SpamhausIpLookupFunction.class)) {
            return configuration.spamhausEnabled();
        }
        if (function.getClass().equals(AbuseChRansomIpLookupFunction.class)) {
            return configuration.abusechRansomEnabled();
        }
        if (function.getClass().equals(OTXIPLookupFunction.class)) {
            return configuration.otxEnabled();
        }
        return true;
    }

    @Override
    public FunctionDescriptor<GlobalLookupResult> descriptor() {
        return FunctionDescriptor.<GlobalLookupResult>builder()
                .name(NAME)
                .description("Match an IP address against all enabled threat intel sources. (except OTX)")
                .params(valueParam, prefixParam)
                .returnType(GlobalLookupResult.class)
                .build();
    }

}
