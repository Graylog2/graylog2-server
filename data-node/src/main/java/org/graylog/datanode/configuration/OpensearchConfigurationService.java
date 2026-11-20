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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.opensearch.OpensearchConfigurationChangeEvent;
import org.graylog.datanode.opensearch.OpensearchStartRequestedEvent;
import org.graylog.datanode.opensearch.configuration.OpensearchConfiguration;
import org.graylog.datanode.opensearch.configuration.OpensearchConfigurationParams;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationBean;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.graylog2.configuration.managed.ManagedConfigurationApplier;
import org.graylog2.configuration.managed.NodeConfigurationReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class OpensearchConfigurationService extends AbstractIdleService {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchConfigurationService.class);
    private static final String NODE_ROLES = "node_roles";

    private final Configuration localConfiguration;
    private final DatanodeConfigurationProvider datanodeConfigurationProvider;
    private final Set<DatanodeConfigurationBean<OpensearchConfigurationParams>> opensearchConfigurationBeans;
    private final EventBus eventBus;
    private final OpensearchUpgradeAction opensearchUpgradeAction;
    private final ManagedConfigurationApplier managedConfigurationApplier;
    private final NodeConfigurationReporter nodeConfigurationReporter;

    @Inject
    public OpensearchConfigurationService(final Configuration localConfiguration,
                                          final DatanodeConfigurationProvider datanodeConfigurationProvider,
                                          final Set<DatanodeConfigurationBean<OpensearchConfigurationParams>> opensearchConfigurationBeans,
                                          final EventBus eventBus,
                                          OpensearchUpgradeAction  opensearchUpgradeAction,
                                          final ManagedConfigurationApplier managedConfigurationApplier,
                                          final NodeConfigurationReporter nodeConfigurationReporter) {
        this.localConfiguration = localConfiguration;
        this.datanodeConfigurationProvider = datanodeConfigurationProvider;
        this.opensearchConfigurationBeans = opensearchConfigurationBeans;
        this.eventBus = eventBus;
        this.opensearchUpgradeAction = opensearchUpgradeAction;
        this.managedConfigurationApplier = managedConfigurationApplier;
        this.nodeConfigurationReporter = nodeConfigurationReporter;
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
    public void onKeystoreChange(DatanodeCertificateChangedEvent event) {
        // configuration relies on the keystore. Initial change there should rebuild the configuration and restart
        // dependent services
        triggerConfigurationChangedEvent();
    }

    @Subscribe
    public void onStartRequested(OpensearchStartRequestedEvent event) {
        // opensearch is being (re)started. Rebuild the configuration from scratch instead of starting the process
        // with whatever configuration was resolved and cached from before, which may be stale by now.
        triggerConfigurationChangedEvent();
    }

    @Subscribe
    public void onOpensearchVersionChange(OpensearchUpdateEvent event) {
        // configuration relies on the keystore. Initial change there should rebuild the configuration and restart
        // dependent services
        LOG.info("Setting OpenSearch version to latest available");
        final boolean upgraded = opensearchUpgradeAction.upgradeToLatestAvaiable();
        if (upgraded) {
            LOG.info("Triggering configuration change event");
            triggerConfigurationChangedEvent();
        } else {
            LOG.warn("Node can't be upgraded, no newer opensearch version available");
        }
    }

    private OpensearchConfiguration get() {

        // get fresh instance of datanode configuration, some parts (like opensearch version) could change meanwhile
        final DatanodeConfiguration datanodeConfiguration = datanodeConfigurationProvider.get();

        final OpensearchConfigurationDir targetConfigDir = datanodeConfiguration.datanodeDirectories().createUniqueOpensearchProcessConfigurationDir();

        final List<DatanodeConfigurationPart> configurationParts = opensearchConfigurationBeans.stream()
                .map(bean -> bean.buildConfigurationPart(new OpensearchConfigurationParams(datanodeConfiguration, targetConfigDir.configurationRoot())))
                .collect(Collectors.toList());

        return new OpensearchConfiguration(
                datanodeConfiguration.opensearchDistribution(),
                datanodeConfiguration.datanodeDirectories(),
                targetConfigDir,
                localConfiguration.getHostname(),
                localConfiguration.getOpensearchHttpPort(),
                configurationParts
        );
    }

    private void triggerConfigurationChangedEvent() {
        // the process is about to be (re)started, so this is where values stored through the web interface have to
        // reach the configuration beans the opensearch configuration is built from
        managedConfigurationApplier.prepareProcessStart();
        final OpensearchConfiguration configuration = get();
        reportNodeRoles(configuration);
        eventBus.post(new OpensearchConfigurationChangeEvent(configuration));
    }

    /**
     * The roles opensearch starts with can differ from the configured ones, e.g. by the role for searchable snapshots
     * (search or warm, depending on the opensearch version), which is added when a snapshot repository is configured.
     */
    private void reportNodeRoles(OpensearchConfiguration configuration) {
        try {
            nodeConfigurationReporter.reportResolvedValue(NODE_ROLES, String.join(",", configuration.opensearchRoles()));
        } catch (Exception e) {
            LOG.warn("Couldn't report the node roles opensearch is started with.", e);
        }
    }
}
