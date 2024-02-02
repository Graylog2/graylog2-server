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
import type { DataNode } from 'preflight/types';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import type { Attribute, PaginatedListJSON, SearchParams } from 'stores/PaginationTypes';

export const bulkRemoveDataNode = async (entity_ids: string[], selectBackFailedEntities: (entity_ids: string[]) => void) => {
  try {
    const { failures, successfully_performed } = await fetch('POST', qualifyUrl('/datanode/bulk_remove'), { entity_ids });

    if (failures?.length) {
      selectBackFailedEntities(failures.map(({ entity_id }) => entity_id));
    }

    if (failures?.length === entity_ids.length) {
      UserNotification.error(`Removing Data Node failed with status: ${JSON.stringify(failures)}`, 'Could not remove Datanodes.');
    }

    if (successfully_performed) {
      UserNotification.success(`${successfully_performed} Data Node${successfully_performed > 1 ? 's' : ''} removed successfully`);
    }
  } catch (errorThrown) {
    UserNotification.error(`Removing Data Node failed with status: ${errorThrown}`, 'Could not remove some Datanodes.');
  }
};

export const bulkStartDataNode = async (entity_ids: string[], selectBackFailedEntities: (entity_ids: string[]) => void) => {
  try {
    const { failures, successfully_performed } = await fetch('POST', qualifyUrl('/datanode/bulk_start'), { entity_ids });

    if (failures?.length) {
      selectBackFailedEntities(failures.map(({ entity_id }) => entity_id));
    }

    if (failures?.length === entity_ids.length) {
      UserNotification.error(`Starting Data Node failed with status: ${JSON.stringify(failures)}`, 'Could not start Datanodes.');
    }

    if (successfully_performed) {
      UserNotification.success(`${successfully_performed} Data Node${successfully_performed > 1 ? 's' : ''} started successfully`);
    }
  } catch (errorThrown) {
    UserNotification.error(`Starting Data Node failed with status: ${errorThrown}`, 'Could not start Datanodes.');
  }
};

export const bulkStopDataNode = async (entity_ids: string[], selectBackFailedEntities: (entity_ids: string[]) => void) => {
  try {
    const { failures, successfully_performed } = await fetch('POST', qualifyUrl('/datanode/bulk_stop'), { entity_ids });

    if (failures?.length) {
      selectBackFailedEntities(failures.map(({ entity_id }) => entity_id));
    }

    if (failures?.length === entity_ids.length) {
      UserNotification.error(`Stopping Data Node failed with status: ${JSON.stringify(failures)}`, 'Could not stop Datanodes.');
    }

    if (successfully_performed) {
      UserNotification.success(`${successfully_performed} Data Node${successfully_performed > 1 ? 's' : ''} stopped successfully`);
    }
  } catch (errorThrown) {
    UserNotification.error(`Stopping Data Node failed with status: ${errorThrown}`, 'Could not stop Datanodes.');
  }
};

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

const fetchDataNodes = async (params?: SearchParams) => {
  const url = PaginationURL('/system/cluster/datanodes', params?.page, params?.pageSize, params?.query, { sort: params?.sort?.attributeId, order: params?.sort?.direction });

  return fetch('GET', qualifyUrl(url));
};

const useDataNodes = (params: SearchParams, { enabled }: Options = { enabled: true }) : {
  data: {
    elements: Array<DataNode>,
    pagination: PaginatedListJSON,
    attributes: Array<Attribute>
  },
  refetch: () => void,
  isInitialLoading: boolean,
} => {
  const { data, refetch, isInitialLoading } = useQuery(
    ['datanodes'],
    () => fetchDataNodes(params),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading Data Nodes failed with status: ${errorThrown}`,
          'Could not load Data Nodes');
      },
      notifyOnChangeProps: ['data', 'error'],
      refetchInterval: 5000,
      enabled,
    },
  );

  const elements = [{
    cert_valid_until: '2053-11-02T13:20:58',
    error_msg: null,
    hostname: 'datanode1',
    node_id: '3af165ef-87a9-467f-b7db-435f4748eb75',
    short_node_id: '3af165ef',
    status: 'CONNECTED' as any,
    transport_address: 'http://datanode1:9200',
    type: 'DATANODE',
    id: '1',
    is_leader: true,
    is_master: true,
    last_seen: '2053-11-02T13:20:58',
    data_node_status: 'AVAILABLE',
    action_queue: 'STOP',
  }, {
    cert_valid_until: '2053-11-02T13:20:58',
    error_msg: null,
    hostname: 'datanode2',
    node_id: '9597fd2f-9c44-466b-ae47-e49ba54d3aeb',
    short_node_id: '9597fd2f',
    status: 'CONNECTED' as any,
    transport_address: 'http://datanode2:9200',
    type: 'DATANODE',
    id: '2',
    is_leader: false,
    is_master: false,
    last_seen: '2053-11-02T13:20:58',
    data_node_status: 'AVAILABLE',
    action_queue: 'STOP',
  }];

  return ({
    data: {
      attributes: [],
      elements,
      pagination: { total: 0 } as any,
    },
    refetch,
    isInitialLoading,
  });
};

export default useDataNodes;
