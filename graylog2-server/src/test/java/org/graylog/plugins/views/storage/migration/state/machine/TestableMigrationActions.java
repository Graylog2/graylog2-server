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

import jakarta.annotation.Nonnull;
import org.graylog.plugins.views.storage.migration.state.actions.MigrationActions;

import java.util.LinkedHashSet;
import java.util.Set;

public class TestableMigrationActions implements MigrationActions {

    private final boolean caAvailable;
    private final boolean renewalPolicyConfigured;
    private final boolean inplaceMigrationVersionCompatible;
    private final boolean dataDirCompatible;
    private final boolean someCompatibleDatanodesRunning;
    private final boolean datanodesProvisioned;
    private final boolean oldClusterStopped;
    private final boolean allDatanodesAvailable;

    private final Set<TestableAction> triggeredActions = new LinkedHashSet<>();

    public TestableMigrationActions(boolean caAvailable, boolean renewalPolicyConfigured, boolean inplaceMigrationVersionCompatible, boolean dataDirCompatible, boolean someCompatibleDatanodesRunning, boolean datanodesProvisioned, boolean oldClusterStopped, boolean allDatanodesAvailable) {
        this.caAvailable = caAvailable;
        this.renewalPolicyConfigured = renewalPolicyConfigured;
        this.inplaceMigrationVersionCompatible = inplaceMigrationVersionCompatible;
        this.dataDirCompatible = dataDirCompatible;
        this.someCompatibleDatanodesRunning = someCompatibleDatanodesRunning;
        this.datanodesProvisioned = datanodesProvisioned;
        this.oldClusterStopped = oldClusterStopped;
        this.allDatanodesAvailable = allDatanodesAvailable;
    }


    @Override
    public void runDirectoryCompatibilityCheck() {
        triggeredActions.add(TestableAction.runDirectoryCompatibilityCheck);
    }

    @Override
    public boolean isOldClusterStopped() {
        return oldClusterStopped;
    }

    @Override
    public void rollingUpgradeSelected() {

    }

    @Override
    public boolean directoryCompatibilityCheckOk() {
        return dataDirCompatible;
    }

    @Override
    public void stopMessageProcessing() {
        triggeredActions.add(TestableAction.stopMessageProcessing);
    }

    @Override
    public boolean caDoesNotExist() {
        return !caAvailable;
    }

    @Override
    public boolean renewalPolicyDoesNotExist() {
        return !renewalPolicyConfigured;
    }

    @Override
    public boolean caAndRenewalPolicyExist() {
        return caAvailable && renewalPolicyConfigured;
    }

    @Override
    public boolean compatibleDatanodesRunning() {
        return someCompatibleDatanodesRunning;
    }

    @Override
    public void provisionDataNodes() {
        triggeredActions.add(TestableAction.provisionDataNodes);
    }

    @Override
    public void provisionAndStartDataNodes() {

    }

    @Override
    public boolean provisioningFinished() {
        return datanodesProvisioned;
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
    public boolean allDatanodesAvailable() {
        return allDatanodesAvailable;
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
        return inplaceMigrationVersionCompatible;
    }

    @Override
    public void getElasticsearchHosts() {

    }

    @Override
    public void stopDatanodes() {
        triggeredActions.add(TestableAction.stopDatanodes);
    }

    public static TestableMigrationActions initialConfiguration() {
        return new TestableMigrationActions(false, false, false, false, false, false, false, false);
    }

    public TestableMigrationActions withCaAvailable() {
        return withCaAvailable(true);
    }

    public TestableMigrationActions withhoutCa() {
        return withCaAvailable(false);
    }

    public TestableMigrationActions withCaAvailable(boolean value) {
        return new TestableMigrationActions(value, this.renewalPolicyConfigured, this.inplaceMigrationVersionCompatible, this.dataDirCompatible, this.someCompatibleDatanodesRunning, this.datanodesProvisioned, this.oldClusterStopped, this.allDatanodesAvailable);
    }

    public TestableMigrationActions withInPlaceCompatibleVersion(boolean value) {
        return new TestableMigrationActions(this.caAvailable, this.renewalPolicyConfigured, value, this.dataDirCompatible, this.someCompatibleDatanodesRunning, this.datanodesProvisioned, this.oldClusterStopped, this.allDatanodesAvailable);
    }

    public TestableMigrationActions withIncompatibleIndexerVersion() {
        return withInPlaceCompatibleVersion(false);
    }

    public TestableMigrationActions withCompatibleIndexerVersion() {
        return withInPlaceCompatibleVersion(true);
    }

    public TestableMigrationActions withRenewalPolicyConfigured(boolean value) {
        return new TestableMigrationActions(this.caAvailable, value, this.inplaceMigrationVersionCompatible, this.dataDirCompatible, this.someCompatibleDatanodesRunning, this.datanodesProvisioned, this.oldClusterStopped, this.allDatanodesAvailable);
    }

    public TestableMigrationActions withRenewalPolicyConfigured() {
        return withRenewalPolicyConfigured(true);
    }

    public TestableMigrationActions withoutRenewalPolicyConfigured() {
        return withRenewalPolicyConfigured(false);
    }

    public TestableMigrationActions withDataDirCompatible() {
        return withDataDirCompatibility(true);
    }

    public TestableMigrationActions withDataDirIncompatible() {
        return withDataDirCompatibility(false);
    }

    @Nonnull
    private TestableMigrationActions withDataDirCompatibility(boolean dataDirCompatible) {
        return new TestableMigrationActions(this.caAvailable, this.renewalPolicyConfigured, this.inplaceMigrationVersionCompatible, dataDirCompatible, this.someCompatibleDatanodesRunning, this.datanodesProvisioned, this.oldClusterStopped, this.allDatanodesAvailable);
    }

    public TestableMigrationActions withSomeCompatibleDatanodesRunning() {
        return new TestableMigrationActions(this.caAvailable, this.renewalPolicyConfigured, this.inplaceMigrationVersionCompatible, this.dataDirCompatible, true, this.datanodesProvisioned, this.oldClusterStopped, this.allDatanodesAvailable);
    }

    public TestableMigrationActions withDatanodesProvisioned() {
        return new TestableMigrationActions(this.caAvailable, this.renewalPolicyConfigured, this.inplaceMigrationVersionCompatible, this.dataDirCompatible, this.someCompatibleDatanodesRunning, true, this.oldClusterStopped, this.allDatanodesAvailable);
    }

    public TestableMigrationActions withOldClusterStopped() {
        return new TestableMigrationActions(this.caAvailable, this.renewalPolicyConfigured, this.inplaceMigrationVersionCompatible, this.dataDirCompatible, this.someCompatibleDatanodesRunning, this.datanodesProvisioned, true, this.allDatanodesAvailable);
    }

    public TestableMigrationActions withOldClusterStillRunning() {
        return new TestableMigrationActions(this.caAvailable, this.renewalPolicyConfigured, this.inplaceMigrationVersionCompatible, this.dataDirCompatible, this.someCompatibleDatanodesRunning, this.datanodesProvisioned, false, this.allDatanodesAvailable);
    }

    public TestableMigrationActions withAllDatanodesAvailable() {
        return new TestableMigrationActions(this.caAvailable, this.renewalPolicyConfigured, this.inplaceMigrationVersionCompatible, this.dataDirCompatible, this.someCompatibleDatanodesRunning, this.datanodesProvisioned, this.oldClusterStopped, true);
    }

    public TestableMigrationActions withSomeDatanodesUnavailable() {
        return new TestableMigrationActions(this.caAvailable, this.renewalPolicyConfigured, this.inplaceMigrationVersionCompatible, this.dataDirCompatible, this.someCompatibleDatanodesRunning, this.datanodesProvisioned, this.oldClusterStopped, false);
    }

    public AssertableStateMachine bindToStateMachine(MigrationState initialState) {
        return new AssertableStateMachine(MigrationStateMachineBuilder.buildWithTestState(initialState, this), this);
    }

    public Set<TestableAction> getTriggeredActions() {
        return triggeredActions;
    }
}
