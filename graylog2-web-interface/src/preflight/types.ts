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
import type { CONFIGURATION_STEPS, DATA_NODES_STATUS } from 'preflight/Constants';

export type DataNodeStatus = keyof typeof DATA_NODES_STATUS;

export type DataNode = {
  hostname: string,
  id: string,
  is_leader: boolean,
  is_master: boolean,
  last_seen: string,
  node_id: string,
  short_node_id: string,
  transport_address: string,
  type: string,
  status: DataNodeStatus,
  data_node_status?: string,
  cert_valid_until: string | null,
  error_msg?: string,
}

export type DataNodes = Array<DataNode>;

export type DataNodesCA = {
  id: string,
  type: string,
}

export type RenewalPolicy = {
  mode: 'AUTOMATIC' | 'MANUAL',
  certificate_lifetime: string,
}

export type ConfigurationStep = typeof CONFIGURATION_STEPS[keyof typeof CONFIGURATION_STEPS]['key']
