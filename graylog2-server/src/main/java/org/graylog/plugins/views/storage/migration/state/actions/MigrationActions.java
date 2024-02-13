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

import org.graylog.plugins.views.storage.migration.state.machine.MigrationStateMachineContext;

/**
 * Set of callbacks used during the migration in different states.
 */
public interface MigrationActions {
    boolean runDirectoryCompatibilityCheck();

    boolean isOldClusterStopped();

    void rollingUpgradeSelected();

    boolean directoryCompatibilityCheckOk();

    void reindexUpgradeSelected();

    boolean reindexingFinished();

    void reindexOldData();

    void stopMessageProcessing();

    void startMessageProcessing();
    boolean caDoesNotExist();
    boolean removalPolicyDoesNotExist();
    boolean caAndRemovalPolicyExist();

    void provisionDataNodes();

    boolean provisioningFinished();

    void resetMigration();

    void setAuthorizationToken(String authorizationToken);

    void setStateMachineContext(MigrationStateMachineContext context);

    MigrationStateMachineContext getStateMachineContext();
}
