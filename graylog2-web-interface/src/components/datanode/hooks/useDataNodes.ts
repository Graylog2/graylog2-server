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

import { CertificateRenewal, Datanode, SystemDatanodes } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';
import type { Attribute, SearchParams } from 'stores/PaginationTypes';
import type { DataNodes } from 'components/datanode/Types';
import { defaultOnError } from 'util/conditional/onError';

export const bulkRemoveDataNode = async (
  entity_ids: string[],
  selectBackFailedEntities: (entity_ids: string[]) => void,
) => {
  try {
    const { failures, successfully_performed } = await Datanode.bulkRemove({
      entity_ids,
    });

    selectBackFailedEntities([]);

    if (failures?.length) {
      selectBackFailedEntities(failures.map(({ entity_id }) => entity_id));
    }

    if (failures?.length === entity_ids.length) {
      UserNotification.error(
        `Removing Data Node failed with status: ${JSON.stringify(failures)}`,
        'Could not remove Data Nodes.',
      );
    }

    if (successfully_performed) {
      UserNotification.success(
        `${successfully_performed} Data Node${successfully_performed > 1 ? 's' : ''} removed successfully.`,
      );
    }
  } catch (errorThrown) {
    UserNotification.error(`Removing Data Node failed with status: ${errorThrown}`, 'Could not remove Data Nodes.');
  }
};

export const bulkStartDataNode = async (
  entity_ids: string[],
  selectBackFailedEntities: (entity_ids: string[]) => void,
) => {
  try {
    const { failures, successfully_performed } = await Datanode.bulkStart({
      entity_ids,
    });

    selectBackFailedEntities([]);

    if (failures?.length) {
      selectBackFailedEntities(failures.map(({ entity_id }) => entity_id));
    }

    if (failures?.length === entity_ids.length) {
      UserNotification.error(
        `Starting Data Node failed with status: ${JSON.stringify(failures)}`,
        'Could not start Data Nodes.',
      );
    }

    if (successfully_performed) {
      UserNotification.success(
        `${successfully_performed} Data Node${successfully_performed > 1 ? 's' : ''} started successfully.`,
      );
    }
  } catch (errorThrown) {
    UserNotification.error(`Starting Data Node failed with status: ${errorThrown}`, 'Could not start Data Nodes.');
  }
};

export const bulkStopDataNode = async (
  entity_ids: string[],
  selectBackFailedEntities: (entity_ids: string[]) => void,
) => {
  try {
    const { failures, successfully_performed } = await Datanode.bulkStop({ entity_ids });

    selectBackFailedEntities([]);

    if (failures?.length) {
      selectBackFailedEntities(failures.map(({ entity_id }) => entity_id));
    }

    if (failures?.length === entity_ids.length) {
      UserNotification.error(
        `Stopping Data Node failed with status: ${JSON.stringify(failures)}`,
        'Could not stop Data Nodes.',
      );
    }

    if (successfully_performed) {
      UserNotification.success(
        `${successfully_performed} Data Node${successfully_performed > 1 ? 's' : ''} stopped successfully.`,
      );
    }
  } catch (errorThrown) {
    UserNotification.error(`Stopping Data Node failed with status: ${errorThrown}`, 'Could not stop Data Nodes.');
  }
};

export const removeDataNode = async (datanodeId: string) => {
  try {
    await Datanode.removeNode(datanodeId);

    UserNotification.success(`Data Node "${datanodeId}" removed successfully.`);
  } catch (errorThrown) {
    UserNotification.error(`Removing Data Node failed with status: ${errorThrown}`, 'Could not remove the Data Node.');
  }
};

export const startDataNode = async (datanodeId: string) => {
  try {
    await Datanode.startNode(datanodeId);

    UserNotification.success(`Data Node "${datanodeId}" started successfully.`);
  } catch (errorThrown) {
    UserNotification.error(`Starting Data Node failed with status: ${errorThrown}`, 'Could not start the Data Node.');
  }
};

export const stopDataNode = async (datanodeId: string) => {
  try {
    await Datanode.stopNode(datanodeId);

    UserNotification.success(`Data Node "${datanodeId}" stopped successfully.`);
  } catch (errorThrown) {
    UserNotification.error(`Stopping Data Node failed with status: ${errorThrown}`, 'Could not stop the Data Node.');
  }
};

export const rejoinDataNode = async (datanodeId: string) => {
  try {
    await Datanode.resetNode(datanodeId);

    UserNotification.success(`Data Node "${datanodeId}" rejoined successfully.`);
  } catch (errorThrown) {
    UserNotification.error(`Rejoining Data Node failed with status: ${errorThrown}`, 'Could not rejoin the Data Node.');
  }
};

export type UseDataNodesOptions = {
  enabled: boolean;
};

export const renewDatanodeCertificate = (nodeId: string) =>
  CertificateRenewal.initiateCertRenewalForNode(nodeId)
    .then(() => {
      UserNotification.success('Certificate renewed successfully.');
    })
    .catch((error) => {
      UserNotification.error(`Certificate renewal failed with error: ${error}`);
    });

export const fetchDataNodes = async (params: SearchParams): Promise<DataNodeResponse> =>
  SystemDatanodes.dataNodes(
    params.sort?.attributeId as Parameters<typeof SystemDatanodes.dataNodes>[0],
    params.page,
    params.pageSize,
    params.query || undefined,
    params.sort?.direction,
  ).then(({ attributes, pagination, elements }) => ({
    attributes,
    list: elements,
    pagination,
  }));

export const keyFn = (searchParams: SearchParams) => ['datanodes', searchParams];

export type DataNodeResponse = {
  list: DataNodes;
  pagination: Awaited<ReturnType<typeof SystemDatanodes.dataNodes>>['pagination'];
  attributes: Array<Attribute>;
};

const useDataNodes = (
  searchParams: SearchParams = {
    query: '-datanode_status:UNAVAILABLE',
    page: 1,
    pageSize: 0,
    sort: undefined,
  },
  { enabled }: UseDataNodesOptions = { enabled: true },
  refetchInterval: number | false = 5000,
) => {
  const { data, refetch, isLoading, error } = useQuery({
    queryKey: keyFn(searchParams),

    queryFn: () =>
      defaultOnError(
        fetchDataNodes(searchParams),
        'Loading Data Nodes failed with status',
        'Could not load Data Nodes.',
      ),
    notifyOnChangeProps: ['data', 'error'],
    refetchInterval,
    enabled,
  });

  return {
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
    isLoading,
    error,
  };
};

export default useDataNodes;
