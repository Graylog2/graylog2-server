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
import { useStore } from 'stores/connect';
import { ClusterOverviewStore } from 'stores/cluster/ClusterOverviewStore';
import { NodesStore } from 'stores/nodes/NodesStore';
import type { NodeInfo } from 'stores/nodes/NodesStore';
import type { SystemOverview } from 'stores/cluster/types';

export type GraylogNode = NodeInfo & SystemOverview;

export type GraylogClusterNode = {
  nodeName: string;
  type: string;
  role: string;
  nodeInfo: GraylogNode;
};

export type UseGraylogNodesResult = {
  nodes: Array<GraylogClusterNode>;
  isLoading: boolean;
};

const useGraylogNodes = (): UseGraylogNodesResult => {
  const { nodes: graylogNodesStore } = useStore(NodesStore);
  const { clusterOverview: systemInfo } = useStore(ClusterOverviewStore);

  const graylogNodes = Object.values(graylogNodesStore || {}).map((graylogNode) => ({
    nodeName: `${graylogNode?.short_node_id} / ${graylogNode?.hostname}`,
    type: 'Server',
    role: graylogNode?.is_leader ? 'Leader' : 'Non-Leader',
    nodeInfo: {
      ...graylogNode,
      ...(systemInfo || {})[graylogNode?.node_id],
    } as GraylogNode,
  }));

  return {
    nodes: graylogNodes,
    isLoading: !graylogNodesStore || !systemInfo,
  };
};

export default useGraylogNodes;
