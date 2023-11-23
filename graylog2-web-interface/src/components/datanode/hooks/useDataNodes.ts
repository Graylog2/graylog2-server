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
import UserNotification from 'preflight/util/UserNotification';
import type { DataNode } from 'preflight/types';
import fetch from 'logic/rest/FetchProvider';
import type { Attribute, PaginatedListJSON, SearchParams } from 'stores/PaginationTypes';
import PaginationURL from 'util/PaginationURL';

export const removeDataNode = async (datanodeId: string) => {
  try {
    await fetch(
      'DELETE',
      qualifyUrl(`/datanode/${datanodeId}`),
    );

    UserNotification.success(`Datanode "${datanodeId}" removed successfully`);
  } catch (errorThrown) {
    UserNotification.error(`Removing Datanode failed with status: ${errorThrown}`, 'Could not Remove the Datanode.');
  }
};

export const rejoinDataNode = async (datanodeId: string) => {
  try {
    await fetch(
      'POST',
      qualifyUrl(`/datanode/${datanodeId}/reset`),
    );

    UserNotification.success(`Datanode "${datanodeId}" rejoined successfully`);
  } catch (errorThrown) {
    UserNotification.error(`Rejoining Datanode failed with status: ${errorThrown}`, 'Could not Rejoin the Datanode.');
  }
};

type Options = {
  enabled: boolean,
}

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
        UserNotification.error(`Loading datanodes failed with status: ${errorThrown}`,
          'Could not load datanodes');
      },
      notifyOnChangeProps: ['data', 'error'],
      enabled,
    },
  );

  return ({
    data: data || {
      attributes: [],
      elements: [],
      pagination: { total: 0 },
    },
    refetch,
    isInitialLoading,
  });
};

export default useDataNodes;
