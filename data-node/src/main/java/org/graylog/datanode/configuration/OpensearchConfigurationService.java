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
package org.graylog.datanode.configuration;

import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.opensearch.OpensearchConfigurationChangeEvent;
import org.graylog.datanode.opensearch.configuration.OpensearchConfiguration;
import org.graylog.datanode.opensearch.configuration.beans.OpensearchConfigurationBean;
import org.graylog.datanode.opensearch.configuration.beans.OpensearchConfigurationPart;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Singleton
public class OpensearchConfigurationService extends AbstractIdleService {
    private final Configuration localConfiguration;
    private final DatanodeConfiguration datanodeConfiguration;
    private final Set<OpensearchConfigurationBean> opensearchConfigurationBeans;

    /**
     * This configuration won't survive datanode restart. But it can be repeatedly provided to the managed opensearch
     */
    private final Map<String, Object> transientConfiguration = new ConcurrentHashMap<>();

    private final List<X509Certificate> trustedCertificates = new ArrayList<>();
    private final EventBus eventBus;

    @Inject
    public OpensearchConfigurationService(final Configuration localConfiguration,
                                          final DatanodeConfiguration datanodeConfiguration,
                                          final Set<OpensearchConfigurationBean> opensearchConfigurationBeans,
                                          final EventBus eventBus) {
        this.localConfiguration = localConfiguration;
        this.datanodeConfiguration = datanodeConfiguration;
        this.opensearchConfigurationBeans = opensearchConfigurationBeans;
        this.eventBus = eventBus;
        eventBus.register(this);
    }

    @Override
    protected void startUp() {
        triggerConfigurationChangedEvent();
    }

    @Override
    protected void shutDown() {

    }

    @Subscribe
    public void onKeystoreChange(DatanodeKeystoreChangedEvent event) {
        // configuration relies on the keystore. Every change there should rebuild the configuration and restart
        // dependent services
        triggerConfigurationChangedEvent();
    }


    public void setAllowlist(List<String> allowlist, List<X509Certificate> trustedCertificates) {
        this.trustedCertificates.addAll(trustedCertificates);
        setTransientConfiguration("reindex.remote.allowlist", allowlist);
    }

    public void removeAllowlist() {
        removeTransientConfiguration("reindex.remote.allowlist");
    }

    public void setTransientConfiguration(String key, Object value) {
        this.transientConfiguration.put(key, value);
        triggerConfigurationChangedEvent();
    }

    public void removeTransientConfiguration(String key) {
        final Object removedValue = this.transientConfiguration.remove(key);
        if (removedValue != null) {
            triggerConfigurationChangedEvent();
        }
    }

    private OpensearchConfiguration get() {

        ImmutableMap.Builder<String, Object> opensearchProperties = ImmutableMap.builder();
        opensearchProperties.putAll(commonOpensearchConfig(localConfiguration));
        final Set<OpensearchConfigurationPart> configurationParts = opensearchConfigurationBeans.stream()
                .map(bean -> bean.buildConfigurationPart(trustedCertificates))
                .collect(Collectors.toSet());

        return new OpensearchConfiguration(
                datanodeConfiguration.opensearchDistributionProvider().get(),
                datanodeConfiguration.datanodeDirectories(),
                localConfiguration.getBindAddress(),
                localConfiguration.getHostname(),
                localConfiguration.getOpensearchHttpPort(),
                localConfiguration.getOpensearchTransportPort(),
                localConfiguration.getClustername(),
                localConfiguration.getDatanodeNodeName(),
                localConfiguration.getNodeRoles(),
                configurationParts,
                opensearchProperties.build()
        );
    }

    private ImmutableMap<String, Object> commonOpensearchConfig(final Configuration localConfiguration) {
        final ImmutableMap.Builder<String, Object> config = ImmutableMap.builder();
        localConfiguration.getOpensearchNetworkHost().ifPresent(
                networkHost -> config.put("network.host", networkHost));
        config.put("path.data", datanodeConfiguration.datanodeDirectories().getDataTargetDir().toString());
        config.put("path.logs", datanodeConfiguration.datanodeDirectories().getLogsTargetDir().toString());

        config.put("network.bind_host", localConfiguration.getBindAddress());

        config.put("network.publish_host", localConfiguration.getHostname());

        if (localConfiguration.getOpensearchDebug() != null && !localConfiguration.getOpensearchDebug().isBlank()) {
            config.put("logger.org.opensearch", localConfiguration.getOpensearchDebug());
        }

        // common OpenSearch config parameters from our docs
        config.put("indices.query.bool.max_clause_count", localConfiguration.getIndicesQueryBoolMaxClauseCount().toString());


        config.putAll(transientConfiguration);

        return config.build();
    }

    private void triggerConfigurationChangedEvent() {
        eventBus.post(new OpensearchConfigurationChangeEvent(get()));
    }


}
