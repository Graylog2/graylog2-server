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
 * - `outdated` — at least one node has a newer OpenSearch version available.
 * - `unconfirmed` — versions look equal, but a down node's version metadata can't be trusted.
 * - `up-to-date` — every node is available and reports no newer version.
 * The remaining states (`upgrading`, `checking`, `error`) mean what they say.
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
  // Outranks everything: versions already read as equal while the job is still finalizing, and fetch
  // errors / missing nodes are normal operating conditions of a rolling upgrade.
  if (hasActiveRollingRestart) {
    return 'upgrading';
  }

  if (isCheckingVersions || isCheckingRollingRestart) {
    return 'checking';
  }

  if (isVersionsError) {
    return 'error';
  }

  if (isUpgradeAvailable) {
    return 'outdated';
  }

  // A down node may come back with a different OpenSearch version than its metadata claims.
  if (unavailableDataNodeCount > 0) {
    return 'unconfirmed';
  }

  return 'up-to-date';
};

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
