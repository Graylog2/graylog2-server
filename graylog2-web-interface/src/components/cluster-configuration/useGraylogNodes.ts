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
import { useMemo } from 'react';

import { useStore } from 'stores/connect';
import { ClusterOverviewStore } from 'stores/cluster/ClusterOverviewStore';
import { NodesStore } from 'stores/nodes/NodesStore';
import type { NodeInfo } from 'stores/nodes/NodesStore';
import type { SystemOverview } from 'stores/cluster/types';

import useAddMetricsToGraylogNodes, { type GraylogNodeMetrics } from './useAddMetricsToGraylogNodes';

export type GraylogNode = NodeInfo &
  Partial<SystemOverview> & {
    id: string;
    metrics: GraylogNodeMetrics;
  };

export type UseGraylogNodesResult = {
  nodes: Array<GraylogNode>;
  isLoading: boolean;
};

const useGraylogNodes = (): UseGraylogNodesResult => {
  const { nodes: graylogNodesStore } = useStore(NodesStore);
  const { clusterOverview: systemInfo } = useStore(ClusterOverviewStore);

  const baseGraylogNodes = useMemo<Array<NodeInfo & Partial<SystemOverview> & { id: string }>>(() => {
    const nodesFromStore = Object.values(graylogNodesStore || {});

    return nodesFromStore.map((graylogNode) => {
      const nodeOverview = systemInfo?.[graylogNode.node_id];
      const combinedNode = {
        ...graylogNode,
        ...nodeOverview,
      };

      return {
        ...combinedNode,
        id: combinedNode.node_id,
      };
    });
  }, [graylogNodesStore, systemInfo]);

  const graylogNodes = useAddMetricsToGraylogNodes(baseGraylogNodes);

  return {
    nodes: graylogNodes,
    isLoading: !graylogNodesStore || !systemInfo,
  };
};

export default useGraylogNodes;
