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

import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.storage.migration.state.actions.MigrationActions;

import java.util.LinkedHashSet;
import java.util.Set;

@AutoValue
public abstract class TestableMigrationActions implements MigrationActions {

    private final Set<TestableAction> triggeredActions = new LinkedHashSet<>();

    abstract boolean caAvailable();
    abstract boolean renewalPolicyConfigured();
    abstract boolean inplaceMigrationVersionCompatible();
    abstract boolean dataDirCompatible();
    abstract boolean someCompatibleDatanodesRunning();
    abstract boolean datanodesProvisioned();
    abstract boolean oldClusterStopped();
    abstract boolean allDatanodesStarted();

    public static Builder initialConfig() {
        return new AutoValue_TestableMigrationActions.Builder()
                .caAvailable(false)
                .renewalPolicyConfigured(false)
                .inplaceMigrationVersionCompatible(false)
                .dataDirCompatible(false)
                .someCompatibleDatanodesRunning(false)
                .datanodesProvisioned(false)
                .oldClusterStopped(false)
                .allDatanodesStarted(false);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder caAvailable(boolean value);
        public abstract Builder renewalPolicyConfigured(boolean value);
        public abstract Builder inplaceMigrationVersionCompatible(boolean value);
        public abstract Builder dataDirCompatible(boolean value);
        public abstract Builder someCompatibleDatanodesRunning(boolean value);
        public abstract Builder datanodesProvisioned(boolean value);
        public abstract Builder oldClusterStopped(boolean value);
        public abstract Builder allDatanodesStarted(boolean value);

        public abstract TestableMigrationActions build();

        public AssertableStateMachine bindToStateMachine(MigrationState initialState) {
            final TestableMigrationActions actions = build();
            return new AssertableStateMachine(MigrationStateMachineBuilder.buildWithTestState(initialState, actions), actions);
        }
    }

    @Override
    public void runDirectoryCompatibilityCheck() {
        triggeredActions.add(TestableAction.runDirectoryCompatibilityCheck);
    }

    @Override
    public boolean isOldClusterStopped() {
        return oldClusterStopped();
    }

    @Override
    public void rollingUpgradeSelected() {

    }

    @Override
    public boolean directoryCompatibilityCheckOk() {
        return dataDirCompatible();
    }

    @Override
    public void stopMessageProcessing() {
        triggeredActions.add(TestableAction.stopMessageProcessing);
    }

    @Override
    public boolean caDoesNotExist() {
        return !caAvailable();
    }

    @Override
    public boolean renewalPolicyDoesNotExist() {
        return !renewalPolicyConfigured();
    }

    @Override
    public boolean caAndRenewalPolicyExist() {
        return caAvailable() && renewalPolicyConfigured();
    }

    @Override
    public boolean compatibleDatanodesRunning() {
        return someCompatibleDatanodesRunning();
    }

    @Override
    public void provisionDataNodes() {
        triggeredActions.add(TestableAction.provisionDataNodes);
    }

    @Override
    public boolean provisioningFinished() {
        return datanodesProvisioned();
    }

    @Override
    public boolean allDatanodesPrepared() {
        return false;
    }

    @Override
    public void startDataNodes() {
        triggeredActions.add(TestableAction.startDataNodes);
    }

    @Override
    public void setPreflightFinished() {
        triggeredActions.add(TestableAction.setPreflightFinished);
    }

    @Override
    public void calculateTrafficEstimate() {
        triggeredActions.add(TestableAction.calculateTrafficEstimate);
    }

    @Override
    public boolean isCompatibleInPlaceMigrationVersion() {
        return inplaceMigrationVersionCompatible();
    }

    @Override
    public boolean allDatanodesAvailable() {
        return allDatanodesStarted();
    }

    @Override
    public void stopDatanodes() {
        triggeredActions.add(TestableAction.stopDatanodes);
    }

    public Set<TestableAction> getTriggeredActions() {
        return triggeredActions;
    }
}
