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
import type { DataNodeInformation } from 'components/datanode/hooks/useDataNodeUpgradeStatus';

// TODO: REMOVE — flip to false (or delete) once the rolling-upgrade status endpoint is wired.
const USE_MOCK_UPGRADE_NODES_FOR_UI_DEV = true;

const mockNode = (index: number, managerNode: boolean, version: string = '2.19.5'): DataNodeInformation => ({
  data_node_status: 'AVAILABLE',
  hostname: `data-node-${index}`,
  opensearch_version: version,
  datanode_version: '7.2.0',
  ip: `192.168.0.${10 + index}`,
  roles: ['cluster_manager', 'data', 'ingest'],
  node_name: `data-node-${index}`,
  upgrade_possible: true,
  manager_node: managerNode,
});

const mockUpgradeNodes: {
  pendingNodes: Array<DataNodeInformation>;
  upgradedNodes: Array<DataNodeInformation>;
  upgradingProgress: number;
} = {
  pendingNodes: [mockNode(2, false), mockNode(3, true)],
  upgradedNodes: [mockNode(1, false, '3.5.0')],
  upgradingProgress: 45,
};

export const upgradeNodesMockOverride = USE_MOCK_UPGRADE_NODES_FOR_UI_DEV ? mockUpgradeNodes : undefined;

export default mockUpgradeNodes;
