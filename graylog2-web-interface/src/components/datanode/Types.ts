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
import type { MIGRATION_STATE } from 'components/datanode/Constants';

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

export type MigrationActions = 'SELECT_MIGRATION' | 'SHOW_DIRECTORY_COMPATIBILITY_CHECK' | 'SHOW_CA_CREATION' | 'SHOW_RENEWAL_POLICY_CREATION' | 'SHOW_MIGRATION_SELECTION' | 'SHOW_DATA_MIGRATION_QUESTION' | 'SHOW_MIGRATE_EXISTING_DATA' | 'SHOW_ASK_TO_SHUTDOWN_OLD_CLUSTER' | 'SHOW_PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES' | 'SHOW_STOP_PROCESSING_PAGE' | 'RUN_DIRECTORY_COMPATIBILITY_CHECK' | 'DIRECTORY_COMPATIBILITY_CHECK_OK' | 'SELECT_ROLLING_UPGRADE_MIGRATION' | 'SELECT_REMOTE_REINDEX_MIGRATION' | 'DISCOVER_NEW_DATANODES' | 'CONFIGURE_DATANODE_CLUSTER' | 'MIGRATE_INDEX_TEMPLATES' | 'MIGRATE_EXISTING_DATA' | 'SKIP_EXISTING_DATA_MIGRATION' | 'MIGRATE_CLUSTER_WITH_DOWNTIME' | 'MIGRATE_CLUSTER_WITHOUT_DOWNTIME' | 'INSTALL_DATANODES_ON_EVERY_NODE' | 'CALCULATE_JOURNAL_SIZE' | 'STOP_MESSAGE_PROCESSING' | 'CONFIRM_OLD_CLUSTER_STOPPED' | 'START_DATANODE_CLUSTER' | 'START_MESSAGE_PROCESSING' | 'CONFIRM_OLD_CONNECTION_STRING_FROM_CONFIG_REMOVED';

type RemoveDescriptionField<T> = {
  [K in keyof T as Exclude<K, 'description'>]: T[K]
};

type ExtractKeyValues<T extends object> = {
  [K in keyof T ]: T[K] extends object ? ExtractKeyValues<RemoveDescriptionField<T[K]>> : T[K];
}[keyof T];

export type MigrationStateItem = ExtractKeyValues<typeof MIGRATION_STATE>

export type MigrationState = {
  state: MigrationStateItem,
  next_steps: Array<MigrationActions>,
}

export type MigrationStepRequest = {
  step: MigrationActions,
  args: {
    [key: string]: unknown;
  };
};
export type StepArgs = {[_key: string]: unknown};

export type OnTriggerStepFunction = (step: MigrationActions, args: StepArgs) => void
export type MigrationStepComponentProps = {
  nextSteps: Array<MigrationActions>,
  onTriggerStep: OnTriggerStepFunction,
};
