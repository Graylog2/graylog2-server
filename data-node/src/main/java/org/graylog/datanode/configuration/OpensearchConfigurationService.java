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
import org.graylog.datanode.opensearch.configuration.OpensearchConfiguration;
import org.graylog.datanode.opensearch.configuration.OpensearchConfigurationParams;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationBean;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class OpensearchConfigurationService extends AbstractIdleService {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchConfigurationService.class);

    private final Configuration localConfiguration;
    private final DatanodeConfigurationProvider datanodeConfigurationProvider;
    private final Set<DatanodeConfigurationBean<OpensearchConfigurationParams>> opensearchConfigurationBeans;
    private final EventBus eventBus;
    private final OpensearchUpgradeAction opensearchUpgradeAction;

    @Inject
    public OpensearchConfigurationService(final Configuration localConfiguration,
                                          final DatanodeConfigurationProvider datanodeConfigurationProvider,
                                          final Set<DatanodeConfigurationBean<OpensearchConfigurationParams>> opensearchConfigurationBeans,
                                          final EventBus eventBus,
                                          OpensearchUpgradeAction  opensearchUpgradeAction) {
        this.localConfiguration = localConfiguration;
        this.datanodeConfigurationProvider = datanodeConfigurationProvider;
        this.opensearchConfigurationBeans = opensearchConfigurationBeans;
        this.eventBus = eventBus;
        this.opensearchUpgradeAction = opensearchUpgradeAction;
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
        eventBus.post(new OpensearchConfigurationChangeEvent(get()));
    }
}
