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
import { deriveOpenSearchUpgradeStatus } from './useOpenSearchUpgradeStatus';

const settledCluster = {
  hasActiveRollingRestart: false,
  isCheckingRollingRestart: false,
  isCheckingVersions: false,
  isUpgradeAvailable: false,
  isVersionsError: false,
  unavailableDataNodeCount: 0,
};

describe('deriveOpenSearchUpgradeStatus', () => {
  it('reports up to date only when every node is available and no upgrade is available', () => {
    expect(deriveOpenSearchUpgradeStatus(settledCluster)).toBe('up-to-date');
  });

  it('reports outdated when an upgrade is available', () => {
    expect(deriveOpenSearchUpgradeStatus({ ...settledCluster, isUpgradeAvailable: true })).toBe('outdated');
  });

  it('reports outdated even while nodes are unavailable — a node reporting an older version is proof enough', () => {
    expect(
      deriveOpenSearchUpgradeStatus({ ...settledCluster, isUpgradeAvailable: true, unavailableDataNodeCount: 1 }),
    ).toBe('outdated');
  });

  it('reports unconfirmed when versions look equal but nodes are unavailable', () => {
    expect(deriveOpenSearchUpgradeStatus({ ...settledCluster, unavailableDataNodeCount: 1 })).toBe('unconfirmed');
  });

  it('reports upgrading while a rolling upgrade is active, even when versions already read as equal', () => {
    expect(deriveOpenSearchUpgradeStatus({ ...settledCluster, hasActiveRollingRestart: true })).toBe('upgrading');
  });

  it('lets an active upgrade outrank unavailable nodes and version fetch errors', () => {
    expect(
      deriveOpenSearchUpgradeStatus({
        ...settledCluster,
        hasActiveRollingRestart: true,
        isVersionsError: true,
        unavailableDataNodeCount: 1,
      }),
    ).toBe('upgrading');
  });

  it('reports checking until both the versions overview and the job state are known', () => {
    expect(deriveOpenSearchUpgradeStatus({ ...settledCluster, isCheckingVersions: true })).toBe('checking');
    expect(deriveOpenSearchUpgradeStatus({ ...settledCluster, isCheckingRollingRestart: true })).toBe('checking');
  });

  it('reports an error when the versions overview failed and no upgrade is running', () => {
    expect(deriveOpenSearchUpgradeStatus({ ...settledCluster, isVersionsError: true })).toBe('error');
  });
});
