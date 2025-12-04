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

import useAddMetricsToGraylogNodes, { type GraylogNodeMetrics } from './useAddMetricsToGraylogNodes';
import useGraylogNodes, { type GraylogNode as BaseGraylogNode, type GraylogNodesResponse } from './useGraylogNodes';

export type GraylogNode = BaseGraylogNode & { metrics: GraylogNodeMetrics };

export type ClusterGraylogNodeResponse = Omit<GraylogNodesResponse, 'list'> & {
  list: Array<GraylogNode>;
};

export type UseClusterGraylogNodesResult = {
  data: ClusterGraylogNodeResponse;
  nodes: Array<GraylogNode>;
  total: number;
  refetch: () => void;
  isLoading: boolean;
  error: unknown;
  pollingEnabled: boolean;
  setPollingEnabled: Dispatch<SetStateAction<boolean>>;
};

const GRAYLOG_NODES_REFETCH_INTERVAL = 5000;

type UseClusterGraylogNodesOptions = {
  refetchInterval?: number | false;
  initialPollingEnabled?: boolean;
  enabled?: boolean;
};

const useClusterGraylogNodes = (
  searchParams?: SearchParams,
  options?: UseClusterGraylogNodesOptions,
): UseClusterGraylogNodesResult => {
  const {
    enabled = true,
    refetchInterval = GRAYLOG_NODES_REFETCH_INTERVAL,
    initialPollingEnabled = true,
  } = options ?? {};
  const [pollingEnabled, setPollingEnabled] = useState(initialPollingEnabled);
  const effectiveRefetchInterval = pollingEnabled ? refetchInterval : false;

  const { data, refetch, isLoading, error } = useGraylogNodes(searchParams, { enabled }, effectiveRefetchInterval);

  const nodesWithMetrics = useAddMetricsToGraylogNodes(data.list);

  const dataWithMetrics = useMemo<ClusterGraylogNodeResponse>(
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

export default useClusterGraylogNodes;
