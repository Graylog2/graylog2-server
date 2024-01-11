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
import type { DataNodeStatus } from 'preflight/types';

type Shard = {
  documents_count: number,
  name: string,
  primary: boolean,
};

type Indice = {
  index_id: string,
  shards:Array<Shard>,
  index_name: string,
  creation_date: string,
  index_version_created: string,
};

export type NodeInfo = {
  indices: Array<Indice>,
  node_version: string,
}
export type CompatibilityResponseType = {
  opensearch_version: string,
  info: {
    nodes: Array<NodeInfo>,
    opensearch_data_location: string,
  },
  compatibility_errors: Array<string>,
};
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
