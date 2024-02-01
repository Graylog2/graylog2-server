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
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.graylog2.plugin.cluster.ClusterConfigService;

public class MigrationActionsImpl implements MigrationActions {

    private final ClusterConfigService clusterConfigService;
    private final CaService caService;

    @Inject
    public MigrationActionsImpl(final ClusterConfigService clusterConfigService,
                                final CaService caService) {
        this.clusterConfigService = clusterConfigService;
        this.caService = caService;
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
}
