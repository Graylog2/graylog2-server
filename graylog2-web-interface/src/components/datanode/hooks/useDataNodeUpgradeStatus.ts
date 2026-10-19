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
import { useQuery } from '@tanstack/react-query';

import { DatanodeUpgrade } from '@graylog/server-api';

import i18n from 'i18n';
import { defaultOnError } from 'util/conditional/onError';
import UserNotification from 'util/UserNotification';

export interface DataNodeInformation {
  data_node_status: 'UNCONFIGURED' | 'PREPARED' | 'STARTING' | 'AVAILABLE' | 'UNAVAILABLE' | 'REMOVING' | 'REMOVED';
  hostname: string;
  opensearch_version: string;
  datanode_version: string;
  ip: string;
  roles: string[];
  node_name: string;
  upgrade_possible: boolean;
  manager_node: boolean;
}

export const isDataNodeAvailable = (node: DataNodeInformation | undefined) => node?.data_node_status === 'AVAILABLE';
export interface ManagerNode {
  node_uid: string;
  name: string;
}
export interface ClusterState {
  cluster_name: string;
  active_shards: number;
  active_primary_shards: number;
  initializing_shards: number;
  unassigned_shards: number;
  delayed_unassigned_shards: number;
  shard_replication: 'ALL' | 'PRIMARIES';
  manager_node: ManagerNode;
  status: string;
  number_of_nodes: number;
  relocating_shards: number;
  opensearch_nodes: Node[];
}
export interface FlushResponse {
  total: number;
  failed: number;
  successful: number;
}
export interface Version {
  version: string;
}
export interface Node {
  ip: string;
  roles: string[];
  host: string;
  name: string;
  version: string;
}
export interface DatanodeUpgradeStatus {
  cluster_healthy: boolean;
  outdated_nodes: DataNodeInformation[];
  up_to_date_nodes: DataNodeInformation[];
  shard_replication_enabled: boolean;
  cluster_state: ClusterState;
  server_version: Version;
  warnings: string[];
}

export const saveNodeToUpgrade = (node_name: string) => localStorage.setItem('datanode-to-upgrade', node_name);

export const getNodeToUpgrade = () => localStorage.getItem('datanode-to-upgrade');

export const removeSavedNodeToUpgrade = () => localStorage.removeItem('datanode-to-upgrade');

export const stopShardReplication = async (): Promise<FlushResponse> => {
  try {
    const response = await DatanodeUpgrade.stopReplication();

    UserNotification.success(i18n.t('data_node_notifications.stop_replication_success', { ns: 'datanode' }));

    return response;
  } catch (errorThrown) {
    UserNotification.error(
      i18n.t('data_node_notifications.stop_replication_failed', { ns: 'datanode', status: errorThrown }),
      i18n.t('data_node_notifications.stop_replication_failed_title', { ns: 'datanode' }),
    );

    return { total: 0, failed: 0, successful: 0 };
  }
};

export const startShardReplication = async (): Promise<FlushResponse> => {
  try {
    const response = await DatanodeUpgrade.startReplication();

    removeSavedNodeToUpgrade();
    UserNotification.success(i18n.t('data_node_notifications.start_replication_success', { ns: 'datanode' }));

    return response;
  } catch (errorThrown) {
    UserNotification.error(
      i18n.t('data_node_notifications.start_replication_failed', { ns: 'datanode', status: errorThrown }),
      i18n.t('data_node_notifications.start_replication_failed_title', { ns: 'datanode' }),
    );

    return { total: 0, failed: 0, successful: 0 };
  }
};

// The generated types are not exported, so the payload stays typed locally.
const fetchDataNodeUpgradeStatus = () =>
  DatanodeUpgrade.status({ requestShouldExtendSession: false }) as Promise<DatanodeUpgradeStatus>;

const UPGRADE_STATUS_REFETCH_INTERVAL_MS = 5000;
// In steady state (all up to date, replication on, healthy) drop to a slow heartbeat.
const STEADY_STATE_REFETCH_INTERVAL_MS = 30000;

const isSteadyState = (status: DatanodeUpgradeStatus | undefined) =>
  !!status && !status.outdated_nodes?.length && status.shard_replication_enabled && status.cluster_healthy;

const useDataNodeUpgradeStatus = (): {
  data: DatanodeUpgradeStatus;
  refetch: () => void;
  isInitialLoading: boolean;
  error: any;
} => {
  const { data, refetch, isInitialLoading, error } = useQuery({
    queryKey: ['datanode-upgrade-status'],

    queryFn: () =>
      defaultOnError(
        fetchDataNodeUpgradeStatus(),
        i18n.t('data_node_notifications.loading_upgrade_status_failed', { ns: 'datanode' }),
        i18n.t('data_node_notifications.loading_upgrade_status_failed_title', { ns: 'datanode' }),
      ),

    notifyOnChangeProps: ['data', 'error'],
    refetchInterval: (query) =>
      isSteadyState(query.state.data) ? STEADY_STATE_REFETCH_INTERVAL_MS : UPGRADE_STATUS_REFETCH_INTERVAL_MS,
  });

  return {
    data,
    refetch,
    isInitialLoading,
    error,
  };
};

export default useDataNodeUpgradeStatus;
