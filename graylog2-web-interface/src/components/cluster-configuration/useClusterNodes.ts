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
import type { SearchParams } from 'stores/PaginationTypes';

export type GraylogNode = NodeInfo & SystemOverview;

export type ClusterNode<NodeType = GraylogNode | DataNode> = {
  nodeName: string;
  type: string;
  role: string;
  nodeInfo: NodeType;
};

export type ClusterNodes = {
  graylogNodes: ClusterNode<GraylogNode>[];
  dataNodes: ClusterNode<DataNode>[];
  refetchDatanodes: () => void;
  isLoading: boolean;
};

type UseClusterNodesOptions = {
  includeDataNodes?: boolean;
};

const DATA_NODES_SEARCH_PARAMS: SearchParams = {
  query: '',
  page: 1,
  pageSize: 0,
  sort: { attributeId: 'hostname', direction: 'asc' },
};

const useClusterNodes = ({ includeDataNodes = true }: UseClusterNodesOptions = {}): ClusterNodes => {
  const { nodes: _graylogNodes } = useStore(NodesStore);
  const { clusterOverview: systemInfo } = useStore(ClusterOverviewStore);
  const graylogNodes = Object.values(_graylogNodes || {}).map((graylogNode) => ({
    nodeName: `${graylogNode?.short_node_id} / ${graylogNode?.hostname}`,
    type: 'Server',
    role: graylogNode?.is_leader ? 'Leader' : 'Non-Leader',
    nodeInfo: {
      ...graylogNode,
      ...(systemInfo || {})[graylogNode?.node_id],
    },
  }));

  const {
    data: _dataNodes,
    refetch: refetchDatanodes,
    isInitialLoading: isDatanodeLoading,
  } = useDataNodes(DATA_NODES_SEARCH_PARAMS, { enabled: includeDataNodes }, includeDataNodes ? undefined : false);
  const dataNodes = includeDataNodes
    ? (_dataNodes?.list || []).map((dataNode) => ({
        nodeName: dataNode?.hostname,
        type: 'Data Node - OpenSearch',
        role: (dataNode?.opensearch_roles || []).join(','),
        nodeInfo: dataNode,
      }))
    : [];

  return {
    graylogNodes,
    dataNodes,
    refetchDatanodes: includeDataNodes ? () => { refetchDatanodes(); } : () => {},
    isLoading: (includeDataNodes && isDatanodeLoading) || !_graylogNodes || !systemInfo,
  };
};

export default useClusterNodes;
