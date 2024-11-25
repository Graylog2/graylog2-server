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
import { defaultOnError } from 'util/conditional/onError';

export const bulkRemoveDataNode = async (entity_ids: string[], selectBackFailedEntities: (entity_ids: string[]) => void) => {
  try {
    const { failures, successfully_performed } = await fetch('POST', qualifyUrl('/datanode/bulk_remove'), { entity_ids });

    selectBackFailedEntities([]);

    if (failures?.length) {
      selectBackFailedEntities(failures.map(({ entity_id }) => entity_id));
    }

    if (failures?.length === entity_ids.length) {
      UserNotification.error(`Removing Data Node failed with status: ${JSON.stringify(failures)}`, 'Could not remove Data Nodes.');
    }

    if (successfully_performed) {
      UserNotification.success(`${successfully_performed} Data Node${successfully_performed > 1 ? 's' : ''} removed successfully.`);
    }
  } catch (errorThrown) {
    UserNotification.error(`Removing Data Node failed with status: ${errorThrown}`, 'Could not remove Data Nodes.');
  }
};

export const bulkStartDataNode = async (entity_ids: string[], selectBackFailedEntities: (entity_ids: string[]) => void) => {
  try {
    const { failures, successfully_performed } = await fetch('POST', qualifyUrl('/datanode/bulk_start'), { entity_ids });

    selectBackFailedEntities([]);

    if (failures?.length) {
      selectBackFailedEntities(failures.map(({ entity_id }) => entity_id));
    }

    if (failures?.length === entity_ids.length) {
      UserNotification.error(`Starting Data Node failed with status: ${JSON.stringify(failures)}`, 'Could not start Data Nodes.');
    }

    if (successfully_performed) {
      UserNotification.success(`${successfully_performed} Data Node${successfully_performed > 1 ? 's' : ''} started successfully.`);
    }
  } catch (errorThrown) {
    UserNotification.error(`Starting Data Node failed with status: ${errorThrown}`, 'Could not start Data Nodes.');
  }
};

export const bulkStopDataNode = async (entity_ids: string[], selectBackFailedEntities: (entity_ids: string[]) => void) => {
  try {
    const { failures, successfully_performed } = await fetch('POST', qualifyUrl('/datanode/bulk_stop'), { entity_ids });

    selectBackFailedEntities([]);

    if (failures?.length) {
      selectBackFailedEntities(failures.map(({ entity_id }) => entity_id));
    }

    if (failures?.length === entity_ids.length) {
      UserNotification.error(`Stopping Data Node failed with status: ${JSON.stringify(failures)}`, 'Could not stop Data Nodes.');
    }

    if (successfully_performed) {
      UserNotification.success(`${successfully_performed} Data Node${successfully_performed > 1 ? 's' : ''} stopped successfully.`);
    }
  } catch (errorThrown) {
    UserNotification.error(`Stopping Data Node failed with status: ${errorThrown}`, 'Could not stop Data Nodes.');
  }
};

export const removeDataNode = async (datanodeId: string) => {
  try {
    await fetch('DELETE', qualifyUrl(`/datanode/${datanodeId}`));

    UserNotification.success(`Data Node "${datanodeId}" removed successfully.`);
  } catch (errorThrown) {
    UserNotification.error(`Removing Data Node failed with status: ${errorThrown}`, 'Could not remove the Data Node.');
  }
};

export const startDataNode = async (datanodeId: string) => {
  try {
    await fetch('POST', qualifyUrl(`/datanode/${datanodeId}/start`));

    UserNotification.success(`Data Node "${datanodeId}" started successfully.`);
  } catch (errorThrown) {
    UserNotification.error(`Starting Data Node failed with status: ${errorThrown}`, 'Could not start the Data Node.');
  }
};

export const stopDataNode = async (datanodeId: string) => {
  try {
    await fetch('POST', qualifyUrl(`/datanode/${datanodeId}/stop`));

    UserNotification.success(`Data Node "${datanodeId}" stopped successfully.`);
  } catch (errorThrown) {
    UserNotification.error(`Stopping Data Node failed with status: ${errorThrown}`, 'Could not stop the Data Node.');
  }
};

export const rejoinDataNode = async (datanodeId: string) => {
  try {
    await fetch('POST', qualifyUrl(`/datanode/${datanodeId}/reset`));

    UserNotification.success(`Data Node "${datanodeId}" rejoined successfully.`);
  } catch (errorThrown) {
    UserNotification.error(`Rejoining Data Node failed with status: ${errorThrown}`, 'Could not rejoin the Data Node.');
  }
};

type Options = {
  enabled: boolean,
}

export const renewDatanodeCertificate = (nodeId: string) => fetch('POST', qualifyUrl(`/certrenewal/${nodeId}`))
  .then(() => {
    UserNotification.success('Certificate renewed successfully.');
  })
  .catch((error) => {
    UserNotification.error(`Certificate renewal failed with error: ${error}`);
  });

export const fetchDataNodes = async (params: SearchParams) => {
  const url = PaginationURL('/system/cluster/datanodes', params.page, params.pageSize, params.query, { sort: params.sort?.attributeId, order: params.sort?.direction });

  return fetch('GET', qualifyUrl(url)).then(({ attributes, pagination, elements }) => ({
    attributes,
    list: elements,
    pagination,
  }));
};

export const keyFn = (searchParams: SearchParams) => ['datanodes', searchParams];

export type DataNodeResponse = {
  list: DataNodes,
  pagination: PaginatedResponseType,
  attributes: Array<Attribute>
}

const useDataNodes = (searchParams: SearchParams = {
  query: '-datanode_status:UNAVAILABLE',
  page: 1,
  pageSize: 0,
  sort: undefined,
}, { enabled }: Options = { enabled: true }, refetchInterval : number | false = 5000) : {
  data: DataNodeResponse,
  refetch: () => void,
  isInitialLoading: boolean,
  error: FetchError,
} => {
  const { data, refetch, isInitialLoading, error } = useQuery<DataNodeResponse, FetchError>(
    keyFn(searchParams),
    () => defaultOnError(fetchDataNodes(searchParams), 'Loading Data Nodes failed with status', 'Could not load Data Nodes.'),
    {
      notifyOnChangeProps: ['data', 'error'],
      refetchInterval,
      enabled,
    },
  );

  return ({
    data: data || {
      attributes: [],
      list: [],
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
