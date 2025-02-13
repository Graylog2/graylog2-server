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
package org.graylog.plugins.views.storage.migration.state.machine;

import org.graylog.plugins.views.storage.migration.state.actions.MigrationActions;

public class MigrationActionsAdapter implements MigrationActions {

    protected final MigrationStateMachineContext context;

    public MigrationActionsAdapter(MigrationStateMachineContext context) {
        this.context = context;
    }

    @Override
    public boolean allDatanodesAvailable() {
        return false;
    }

    @Override
    public void setPreflightFinished() {

    }

    @Override
    public void startRemoteReindex() {

    }

    @Override
    public void requestMigrationStatus() {

    }

    @Override
    public void calculateTrafficEstimate() {

    }

    @Override
    public void verifyRemoteIndexerConnection() {

    }

    @Override
    public boolean isCompatibleInPlaceMigrationVersion() {
        return true;
    }

    @Override
    public void getElasticsearchHosts() {

    }

    @Override
    public void stopDatanodes() {
    }

    @Override
    public void finishRemoteReindexMigration() {
    }

    @Override
    public boolean isRemoteReindexMigrationEnabled() {
        return true;
    }

    @Override
    public void runDirectoryCompatibilityCheck() {
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
    public boolean isRemoteReindexingFinished() {
        return false;
    }

    @Override
    public void stopMessageProcessing() {

    }

    @Override
    public void startMessageProcessing() {

    }

    @Override
    public boolean caDoesNotExist() {
        return false;
    }

    @Override
    public boolean renewalPolicyDoesNotExist() {
        return false;
    }

    @Override
    public boolean caAndRenewalPolicyExist() {
        return false;
    }

    @Override
    public boolean compatibleDatanodesRunning() {
        return false;
    }

    @Override
    public void provisionDataNodes() {
    }

    @Override
    public void provisionAndStartDataNodes() {
    }

    @Override
    public boolean provisioningFinished() {
        return false;
    }

    @Override
    public void startDataNodes() {

    }

    @Override
    public boolean allDatanodesPrepared() {
        return false;
    }
}
