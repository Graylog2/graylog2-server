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
package org.graylog.plugins.views.storage.migration.state.actions;

import jakarta.inject.Inject;
import org.graylog.plugins.views.storage.migration.state.persistence.DatanodeMigrationConfiguration;
import org.graylog.security.certutil.CaService;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.cluster.preflight.DataNodeProvisioningConfig;
import org.graylog2.cluster.preflight.DataNodeProvisioningService;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.graylog2.plugin.cluster.ClusterConfigService;

import java.util.Map;

public class MigrationActionsImpl implements MigrationActions {

    private final ClusterConfigService clusterConfigService;
    private final NodeService<DataNodeDto> nodeService;
    private final CaService caService;
    private final DataNodeProvisioningService dataNodeProvisioningService;

    @Inject
    public MigrationActionsImpl(final ClusterConfigService clusterConfigService, NodeService<DataNodeDto> nodeService,
                                final CaService caService, DataNodeProvisioningService dataNodeProvisioningService) {
        this.clusterConfigService = clusterConfigService;
        this.nodeService = nodeService;
        this.caService = caService;
        this.dataNodeProvisioningService = dataNodeProvisioningService;
    }

    @Override
    public void resetMigration() {
        clusterConfigService.remove(DatanodeMigrationConfiguration.class);
    }


    @Override
    public boolean runDirectoryCompatibilityCheck() {
        // TODO: add real test
        return true;
    }

    @Override
    public boolean isOldClusterStopped() {
        // TODO: add real test
        return true;
    }

    @Override
    public void rollingUpgradeSelected() {

    }

    @Override
    public boolean directoryCompatibilityCheckOk() {
        // TODO: add real test
        return true;
    }

    @Override
    public void reindexUpgradeSelected() {

    }

    @Override
    public boolean reindexingFinished() {
        // TODO: add real test
        return true;
    }

    @Override
    public void reindexOldData() {

    }

    @Override
    public void stopMessageProcessing() {

    }

    @Override
    public void startMessageProcessing() {

    }

    @Override
    public boolean caDoesNotExist() {
        try {
            return this.caService.get() == null;
        } catch (KeyStoreStorageException e) {
            return true;
        }
    }

    @Override
    public boolean removalPolicyDoesNotExist() {
        return this.clusterConfigService.get(RenewalPolicy.class) == null;
    }

    @Override
    public boolean caAndRemovalPolicyExist() {
        return !caDoesNotExist() && !removalPolicyDoesNotExist();
    }

    @Override
    public void provisionDataNodes() {
        final Map<String, DataNodeDto> activeDataNodes = nodeService.allActive();
        activeDataNodes.values().forEach(node -> dataNodeProvisioningService.changeState(node.getNodeId(), DataNodeProvisioningConfig.State.CONFIGURED));
    }
}
