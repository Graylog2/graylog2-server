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

import { qualifyUrl } from 'util/URLUtils';
import type { DataNode } from 'preflight/types';
import fetch from 'logic/rest/FetchProvider';
import { defaultOnError } from 'util/conditional/onError';
import UserNotification from 'util/UserNotification';

export const stopShardReplication = async () => {
  try {
    await fetch('POST', qualifyUrl('datanodes/upgrade/replication/stop'));

    UserNotification.success(`Shard replication stopped successfully`);
  } catch (errorThrown) {
    UserNotification.error(`Stopping shard replication failed with status: ${errorThrown}`, 'Could not stop shard replication.');
  }
};

export const startShardReplication = async () => {
  try {
    await fetch('POST', qualifyUrl('datanodes/upgrade/replication/start'));

    UserNotification.success(`Shard replication started successfully`);
  } catch (errorThrown) {
    UserNotification.error(`Starting shard replication failed with status: ${errorThrown}`, 'Could not start shard replication.');
  }
};

const fetchDataNodeUpgradeStatus = async () => fetch('GET', qualifyUrl('/datanodes/upgrade/status'));

const useDataNodeUpgradeStatus = (): {
  data: DataNode;
  refetch: () => void;
  isInitialLoading: boolean;
  error: any;
} => {
  const { data, refetch, isInitialLoading, error } = useQuery(
    ['datanode-upgrade-status'],
    () => defaultOnError(fetchDataNodeUpgradeStatus(), 'Loading Data Node upgrade status failed', 'Could not load Data Node upgrade status'),
    {
      notifyOnChangeProps: ['data', 'error'],
      refetchInterval: 5000,
    },
  );

  return {
    data,
    refetch,
    isInitialLoading,
    error,
  };
};

export default useDataNodeUpgradeStatus;
