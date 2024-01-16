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
import PaginationURL from 'util/PaginationURL';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import type { Attribute, SearchParams, PaginatedResponseType } from 'stores/PaginationTypes';
import type FetchError from 'logic/errors/FetchError';
import type { DataNodes } from 'components/datanode/Types';

export const removeDataNode = async (datanodeId: string) => {
  try {
    await fetch('DELETE', qualifyUrl(`/datanode/${datanodeId}`));

    UserNotification.success(`Data Node "${datanodeId}" removed successfully`);
  } catch (errorThrown) {
    UserNotification.error(`Removing Data Node failed with status: ${errorThrown}`, 'Could not remove the Data Node.');
  }
};

export const startDataNode = async (datanodeId: string) => {
  try {
    await fetch('POST', qualifyUrl(`/datanode/${datanodeId}/start`));

    UserNotification.success(`Data Node "${datanodeId}" started successfully`);
  } catch (errorThrown) {
    UserNotification.error(`Starting Data Node failed with status: ${errorThrown}`, 'Could not start the Data Node.');
  }
};

export const stopDataNode = async (datanodeId: string) => {
  try {
    await fetch('POST', qualifyUrl(`/datanode/${datanodeId}/stop`));

    UserNotification.success(`Data Node "${datanodeId}" stopped successfully`);
  } catch (errorThrown) {
    UserNotification.error(`Stopping Data Node failed with status: ${errorThrown}`, 'Could not stop the Data Node.');
  }
};

export const rejoinDataNode = async (datanodeId: string) => {
  try {
    await fetch('POST', qualifyUrl(`/datanode/${datanodeId}/reset`));

    UserNotification.success(`Data Node "${datanodeId}" rejoined successfully`);
  } catch (errorThrown) {
    UserNotification.error(`Rejoining Data Node failed with status: ${errorThrown}`, 'Could not rejoin the Data Node.');
  }
};

type Options = {
  enabled: boolean,
}

export const renewDatanodeCertificate = (nodeId: string) => fetch('POST', qualifyUrl(`/certrenewal/${nodeId}`))
  .then(() => {
    UserNotification.success('Certificate renewed successfully');
  })
  .catch((error) => {
    UserNotification.error(`Certificate renewal failed with error: ${error}`);
  });

const fetchDataNodes = async (params?: Partial<SearchParams>) => {
  const url = PaginationURL('/system/cluster/datanodes', params?.page, params?.pageSize, params?.query, { sort: params?.sort?.attributeId, order: params?.sort?.direction });

  return fetch('GET', qualifyUrl(url));
};

export type DataNodeResponse = {
  elements: DataNodes,
  pagination: PaginatedResponseType,
  attributes: Array<Attribute>
}

const useDataNodes = (params: Partial<SearchParams> = {
  query: '',
  page: 1,
  pageSize: 0,
}, { enabled }: Options = { enabled: true }, refetchInterval : number | false = 5000) : {
  data: DataNodeResponse,
  refetch: () => void,
  isInitialLoading: boolean,
  error: FetchError,
} => {
  const { data, refetch, isInitialLoading, error } = useQuery<DataNodeResponse, FetchError>(
    ['datanodes'],
    () => fetchDataNodes(params),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading Data Nodes failed with status: ${errorThrown}`,
          'Could not load Data Nodes');
      },
      notifyOnChangeProps: ['data', 'error'],
      refetchInterval,
      enabled,
    },
  );

  return ({
    data: data || {
      attributes: [],
      elements: [],
      pagination: {
        query: '',
        page: 1,
        per_page: 0,
        total: 0,
        count: 0,
      },
    },
    refetch,
    isInitialLoading,
    error,
  });
};

export default useDataNodes;
