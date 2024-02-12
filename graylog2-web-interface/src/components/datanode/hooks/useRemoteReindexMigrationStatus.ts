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
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';

export type MigrationStatus = 'NOT_STARTED'|'STARTING'|'RUNNING'|'ERROR'|'FINISHED';

export type RemoteReindexIndex = {
  took: string,
  batches: number,
  error_msg: string,
  created: string,
  name: string,
  status: MigrationStatus,
}

export type RemoteReindexMigration = {
  indices: RemoteReindexIndex[],
  id: string,
  error: string,
  status: MigrationStatus,
}

export type RemoteReindexRequest = {
  hostname: string,
  password: string,
  indices: string[],
  synchronous: boolean,
  user: string,
}

export const remoteReindex = async (request: RemoteReindexRequest) => {
  try {
    const result = await fetch('POST', qualifyUrl('/remote-reindex-migration/remoteReindex'), request);

    UserNotification.success('Successful Remote Reindexing.');

    return result;
  } catch (errorThrown) {
    UserNotification.error(`Remote Reindexing failed with status: ${errorThrown}`, 'Remote Reindexing Failure.');

    return null;
  }
};

const fetchRemoteReindexStatus = async (migrationID: string) => fetch('GET', qualifyUrl(`/remote-reindex-migration/status/${migrationID}`));

const useRemoteReindexMigrationStatus = (migrationID: string) : {
  data: RemoteReindexMigration,
  refetch: () => void,
  isInitialLoading: boolean,
  error: any,
} => {
  const { data, refetch, isInitialLoading, error } = useQuery<RemoteReindexMigration>(
    ['remote-reindex-status'],
    () => fetchRemoteReindexStatus(migrationID),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading Remote Reindex Migration Status failed with status: ${errorThrown}`,
          'Could not load Remote Reindex Migration Status');
      },
      notifyOnChangeProps: ['data', 'error'],
      refetchInterval: 5000,
      enabled: !!migrationID,
    },
  );

  return ({
    data,
    refetch,
    isInitialLoading,
    error,
  });
};

export default useRemoteReindexMigrationStatus;
