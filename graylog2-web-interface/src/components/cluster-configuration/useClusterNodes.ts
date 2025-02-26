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
import useDataNodes from 'components/datanode/hooks/useDataNodes';
import { NodesStore } from 'stores/nodes/NodesStore';

export type ClusterNode = {
  nodeName: string,
  type: string,
  role: string,
  state: string,
}

const useClusterNodes = () : {
  graylogNodes: ClusterNode[],
  dataNodes: ClusterNode[],
} => {
  const { nodes: _graylogNodes } = useStore(NodesStore);
  const graylogNodes = Object.values(_graylogNodes || {}).map((graylogNode) => ({
    nodeName: `${graylogNode.short_node_id} / ${graylogNode.hostname}`,
    type: 'Graylog',
    role: graylogNode.is_leader ? 'Leader' : 'Non-Leader',
    state: 'Available',
  }));

  const { data: _dataNodes } = useDataNodes();
  const dataNodes = (_dataNodes?.list || []).map((dataNode) => ({
    nodeName: dataNode.hostname,
    type: 'Data Node - OpenSearch',
    role: 'Cluster-manager-eligible,Data,Ingest,Coordinator',
    state: 'Available',
  }));

  return ({
    graylogNodes,
    dataNodes,
  });
};

export default useClusterNodes;
