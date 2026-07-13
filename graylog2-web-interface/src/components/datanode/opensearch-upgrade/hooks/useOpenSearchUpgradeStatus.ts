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
import useOpenSearchClusterStats from './useOpenSearchClusterStats';
import { useCurrentRollingRestart } from './useOpenSearchRollingRestart';

import { isRollingRestartActive } from '../rollingRestartTypes';

/**
 * The single verdict on the state of the Data Nodes' embedded OpenSearch:
 * - `upgrading` — a rolling upgrade job is running.
 * - `checking` — the version overview or job state has not loaded yet.
 * - `error` — the version overview could not be loaded.
 * - `outdated` — at least one node has a newer OpenSearch version available.
 * - `unconfirmed` — versions look equal, but unavailable nodes may report stale version metadata.
 * - `up-to-date` — every node is available and reports no newer version.
 */
export type OpenSearchUpgradeStatus = 'upgrading' | 'checking' | 'error' | 'outdated' | 'unconfirmed' | 'up-to-date';

type StatusInputs = {
  hasActiveRollingRestart: boolean;
  isCheckingRollingRestart: boolean;
  isCheckingVersions: boolean;
  isUpgradeAvailable: boolean;
  isVersionsError: boolean;
  unavailableDataNodeCount: number;
};

export const deriveOpenSearchUpgradeStatus = ({
  hasActiveRollingRestart,
  isCheckingRollingRestart,
  isCheckingVersions,
  isUpgradeAvailable,
  isVersionsError,
  unavailableDataNodeCount,
}: StatusInputs): OpenSearchUpgradeStatus => {
  // An active upgrade is the dominant fact: versions already read as equal while the job is still
  // finalizing (e.g. waiting for the cluster to return to GREEN), fetch errors and temporarily
  // missing nodes are its normal operating conditions.
  if (hasActiveRollingRestart) {
    return 'upgrading';
  }

  if (isCheckingVersions || isCheckingRollingRestart) {
    return 'checking';
  }

  if (isVersionsError) {
    return 'error';
  }

  // Outranks `unconfirmed`: a node reporting an older version is proof enough, no matter which
  // other nodes are down.
  if (isUpgradeAvailable) {
    return 'outdated';
  }

  // A node that is down or still starting may come back with a different OpenSearch version than
  // its metadata claims, so neither "up to date" nor "outdated" can be stated honestly.
  if (unavailableDataNodeCount > 0) {
    return 'unconfirmed';
  }

  return 'up-to-date';
};

// Both underlying queries only report loading on their initial fetch, so the status never
// flickers back to `checking` during the 5s background polls.
const useOpenSearchUpgradeStatus = (): OpenSearchUpgradeStatus => {
  const { isError, isLoading, isUpgradeAvailable, unavailableDataNodeCount } = useOpenSearchClusterStats();
  const { data: rollingRestart, isLoading: isLoadingRollingRestart } = useCurrentRollingRestart();

  return deriveOpenSearchUpgradeStatus({
    hasActiveRollingRestart: isRollingRestartActive(rollingRestart),
    isCheckingRollingRestart: isLoadingRollingRestart,
    isCheckingVersions: isLoading,
    isUpgradeAvailable,
    isVersionsError: isError,
    unavailableDataNodeCount,
  });
};

export default useOpenSearchUpgradeStatus;
