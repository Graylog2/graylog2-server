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

import { DatanodeUpgrade } from '@graylog/server-api';

import { defaultOnError } from 'util/conditional/onError';

type OpenSearchVersionsOverview = Awaited<ReturnType<typeof DatanodeUpgrade.opensearchVersions>>;

export type OpenSearchVersionNode = Omit<OpenSearchVersionsOverview['nodes'][number], 'datanode'> & {
  datanode?: OpenSearchVersionsOverview['nodes'][number]['datanode'] | null;
};

const isAvailableDataNode = ({ datanode }: OpenSearchVersionNode) => datanode?.datanode_status === 'AVAILABLE';

const useOpenSearchClusterStats = () => {
  const { data, isError, isFetching, isInitialLoading, refetch } = useQuery({
    queryKey: ['opensearch-upgrade', 'versions-overview'],
    queryFn: () =>
      defaultOnError(
        DatanodeUpgrade.opensearchVersions(),
        'Loading OpenSearch versions overview failed',
        'Could not load OpenSearch versions overview',
      ),
  });
  const nodes: Array<OpenSearchVersionNode> = data?.nodes ?? [];

  return {
    currentVersion: data?.lowest_current_version,
    targetVersion: data?.highest_available_version,
    nodes,
    numberOfDataNodes: nodes.filter(isAvailableDataNode).length,
    isUpgradeAvailable: data?.upgrade_available ?? false,
    isUpToDate: data ? !data.upgrade_available && data.up_to_date_count === data.nodes.length : false,
    isError,
    isFetching,
    isLoading: isInitialLoading,
    refetch,
  };
};

export default useOpenSearchClusterStats;
