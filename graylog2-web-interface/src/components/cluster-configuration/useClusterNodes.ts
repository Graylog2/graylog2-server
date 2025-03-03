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
import type { DataNode } from 'components/datanode/Types';
import useDataNodes from 'components/datanode/hooks/useDataNodes';

export type GraylogNode = NodeInfo & SystemOverview;

export type ClusterNode<NodeType = GraylogNode | DataNode> = {
  nodeName: string,
  type: string,
  role: string,
  nodeInfo: NodeType,
}

export type ClusterNodes = {
  graylogNodes: ClusterNode<GraylogNode>[],
  dataNodes: ClusterNode<DataNode>[],
  refetchDatanodes: () => void,
}

const useClusterNodes = (): ClusterNodes => {
  const { nodes: _graylogNodes } = useStore(NodesStore);
  const { clusterOverview: systemInfo  } = useStore(ClusterOverviewStore);
  const graylogNodes = Object.values(_graylogNodes || {}).map((graylogNode) => ({
    nodeName: `${graylogNode.short_node_id} / ${graylogNode.hostname}`,
    type: 'Graylog',
    role: graylogNode.is_leader ? 'Leader' : 'Non-Leader',
    nodeInfo: {
      ...graylogNode,
      ...systemInfo[graylogNode.node_id]
    },
  }));

  const { data: _dataNodes, refetch: refetchDatanodes } = useDataNodes({ query: '', page: 1, pageSize: 0, sort: { attributeId: 'hostname', direction: 'asc' } });
  const dataNodes = (_dataNodes?.list || []).map((dataNode) => ({
    nodeName: dataNode.hostname,
    type: 'Data Node - OpenSearch',
    role: 'Cluster-manager-eligible,Data,Ingest,Coordinator',
    nodeInfo: dataNode,
  }));

  return ({
    graylogNodes,
    dataNodes,
    refetchDatanodes,
  });
};

export default useClusterNodes;
