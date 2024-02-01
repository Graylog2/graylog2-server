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

import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Inject;
import org.graylog.plugins.views.storage.migration.state.persistence.DatanodeMigrationConfiguration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.system.processing.control.ClusterProcessingControlFactory;

public class MigrationActionsImpl implements MigrationActions {

    private final ClusterConfigService clusterConfigService;
    private final ClusterProcessingControlFactory clusterProcessingControlFactory;
    private String authorizationToken;

    @Inject
    public MigrationActionsImpl(ClusterConfigService clusterConfigService,
                                ClusterProcessingControlFactory clusterProcessingControlFactory) {
        this.clusterConfigService = clusterConfigService;
        this.clusterProcessingControlFactory = clusterProcessingControlFactory;
    }

    @Override
    public void resetMigration() {
        clusterConfigService.remove(DatanodeMigrationConfiguration.class);
    }


    @Override
    public boolean runDirectoryCompatibilityCheck() {
        return false;
    }

    @Override
    public boolean isOldClusterStopped() {
        return false;
    }

    @Override
    public void rollingUpgradeSelected() {

    }

    @Override
    public boolean directoryCompatibilityCheckOk() {
        return false;
    }

    @Override
    public void reindexUpgradeSelected() {

    }

    @Override
    public boolean reindexingFinished() {
        return false;
    }

    @Override
    public void reindexOldData() {

    }

    @Override
    public void stopMessageProcessing() {
        final ClusterProcessingControlFactory.ClusterProcessingControl control = clusterProcessingControlFactory.create(authorizationToken);
        control.pauseProcessing();
    }

    @Override
    public void startMessageProcessing() {
        final ClusterProcessingControlFactory.ClusterProcessingControl control = clusterProcessingControlFactory.create(authorizationToken);
        control.resumeGraylogMessageProcessing();
    }

    @Override
    public boolean caDoesNotExist() {
        return false;
    }

    @Override
    public boolean removalPolicyDoesNotExist() {
        return false;
    }

    @Override
    public boolean caAndRemovalPolicyExist() {
        return false;
    }

    @Override
    public void setAuthorizationToken(String authorizationToken) {
        this.authorizationToken = authorizationToken;
    }

}
