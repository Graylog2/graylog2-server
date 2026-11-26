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
import { Datanode, DataNodeRollingRestart, SystemManagedConfiguration } from '@graylog/server-api';

import type { ManagedConfiguration, ConfigurationValues } from './types';

const NODE_TYPE = 'datanode';

const managedConfigurationApi = {
  get: (nodeId: string | null = null): Promise<ManagedConfiguration> =>
    SystemManagedConfiguration.get(NODE_TYPE, nodeId ?? undefined),
  update: (values: ConfigurationValues, nodeId: string | null = null): Promise<ManagedConfiguration> =>
    SystemManagedConfiguration.update({ node_type: NODE_TYPE, node_id: nodeId, values }),
  // A single Data Node restarts its OpenSearch process right away. All Data Nodes are restarted one after another, so
  // the cluster stays available, without upgrading OpenSearch.
  restart: (nodeId: string | null = null, force = false): Promise<unknown> =>
    nodeId === null ? DataNodeRollingRestart.start({ force, restart_only: true }) : Datanode.restartNode(nodeId),
};

export default managedConfigurationApi;
