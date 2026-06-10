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
import { useQuery } from '@tanstack/react-query';

import { SystemClusterStats } from '@graylog/server-api';

import { defaultOnError } from 'util/conditional/onError';

export const TARGET_OPENSEARCH_VERSION = '3.5.0';

const versionParts = (version: string) => version.split(/[+-]/)[0].split('.').map((part) => Number.parseInt(part, 10));

export const isOpenSearchVersionUpToDate = (currentVersion: string | undefined) => {
  if (!currentVersion) {
    return false;
  }

  const [currentMajor = 0, currentMinor = 0, currentPatch = 0] = versionParts(currentVersion);
  const [targetMajor = 0, targetMinor = 0, targetPatch = 0] = versionParts(TARGET_OPENSEARCH_VERSION);

  if (currentMajor !== targetMajor) {
    return currentMajor > targetMajor;
  }

  if (currentMinor !== targetMinor) {
    return currentMinor > targetMinor;
  }

  return currentPatch >= targetPatch;
};

const useOpenSearchClusterStats = () => {
  const { data, isInitialLoading } = useQuery({
    queryKey: ['opensearch-upgrade', 'cluster-stats'],
    queryFn: () =>
      defaultOnError(
        SystemClusterStats.elasticsearchStats(),
        'Loading OpenSearch cluster stats failed',
        'Could not load OpenSearch cluster stats',
      ),
  });

  return {
    currentVersion: data?.cluster_version,
    numberOfDataNodes: data?.cluster_health?.number_of_data_nodes ?? 0,
    isLoading: isInitialLoading,
  };
};

export default useOpenSearchClusterStats;
