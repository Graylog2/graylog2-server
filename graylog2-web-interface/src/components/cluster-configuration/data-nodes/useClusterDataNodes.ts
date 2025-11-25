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
import { useMemo, useState, type Dispatch, type SetStateAction } from 'react';

import type { SearchParams } from 'stores/PaginationTypes';
import type { DataNode } from 'components/datanode/Types';
import useDataNodes, { type DataNodeResponse } from 'components/datanode/hooks/useDataNodes';
import useShowDatanodeMigration from 'components/datanode/hooks/useShowDatanodeMigration';

import useAddMetricsToDataNodes, { type DataNodeMetrics } from './useAddMetricsToDataNodes';

export type ClusterDataNode = DataNode & { metrics: DataNodeMetrics };

export type ClusterDataNodeResponse = Omit<DataNodeResponse, 'list'> & {
  list: Array<ClusterDataNode>;
};

export type UseClusterDataNodesResult = {
  data: ClusterDataNodeResponse;
  nodes: Array<ClusterDataNode>;
  total: number;
  refetch: () => void;
  isLoading: boolean;
  error: unknown;
  pollingEnabled: boolean;
  setPollingEnabled: Dispatch<SetStateAction<boolean>>;
};

const DATANODES_REFETCH_INTERVAL = 5000;

type UseClusterDataNodesOptions = {
  refetchInterval?: number | false;
  initialPollingEnabled?: boolean;
  enabled?: boolean;
};

const useClusterDataNodes = (
  searchParams?: SearchParams,
  options?: UseClusterDataNodesOptions,
): UseClusterDataNodesResult => {
  const {
    enabled = true,
    refetchInterval = DATANODES_REFETCH_INTERVAL,
    initialPollingEnabled = true,
  } = options ?? {};
  const [pollingEnabled, setPollingEnabled] = useState(initialPollingEnabled);
  const effectiveRefetchInterval = pollingEnabled ? refetchInterval : false;
  const { isDatanodeConfiguredAndUsed } = useShowDatanodeMigration();

  const {
    data,
    refetch,
    isLoading,
    error,
  } = useDataNodes(searchParams, { enabled }, effectiveRefetchInterval);

  const nodesWithMetrics = useAddMetricsToDataNodes(data.list, {
    refetchInterval: effectiveRefetchInterval,
    enabled: enabled && isDatanodeConfiguredAndUsed,
  });

  const dataWithMetrics = useMemo<ClusterDataNodeResponse>(
    () => ({
      ...data,
      list: nodesWithMetrics,
    }),
    [data, nodesWithMetrics],
  );

  const nodes = dataWithMetrics.list;
  const total = dataWithMetrics.pagination?.total ?? nodes.length;

  return {
    data: dataWithMetrics,
    nodes,
    total,
    refetch,
    isLoading,
    error,
    pollingEnabled,
    setPollingEnabled,
  };
};

export default useClusterDataNodes;
