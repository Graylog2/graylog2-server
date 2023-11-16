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
import { fetchPeriodically } from 'logic/rest/FetchProvider';
import type { DataNode, DataNodeStatus } from 'preflight/types';

export const fetchDataNodes = () => fetchPeriodically<Array<DataNode>>('GET', qualifyUrl('/certrenewal'));

const useDataNodes = () => {
  const { data, isInitialLoading } = useQuery({
    queryKey: ['data-nodes', 'overview'],
    queryFn: fetchDataNodes,
    onError: (errorThrown) => {
      UserNotification.error(`Loading data nodes failed with status: ${errorThrown}`,
        'Could not load datanodes');
    },
    keepPreviousData: true,
    refetchInterval: 3000,

  });

  const mockData = [
    {
      id: '1',
      is_leader: false,
      is_master: false,
      last_seen: '2023-11-02T13:20:58',
      cert_valid_until: '2053-11-02T13:20:58',
      error_msg: null,
      hostname: 'datanode1',
      node_id: '3af165ef-87a9-467f-b7db-435f4748eb75',
      short_node_id: '3af165ef',
      status: 'CONNECTED' as DataNodeStatus,
      transport_address: 'http://datanode1:9200',
      type: 'DATANODE',
    },
    {
      id: '2',
      is_leader: false,
      is_master: false,
      last_seen: '2023-11-02T13:20:58',
      cert_valid_until: '2053-11-02T13:20:58',
      error_msg: null,
      hostname: 'datanode2',
      node_id: '9597fd2f-9c44-466b-ae47-e49ba54d3aeb',
      short_node_id: '9597fd2f',
      status: 'CONNECTED' as DataNodeStatus,
      transport_address: 'http://datanode2:9200',
      type: 'DATANODE',
    },
  ];

  return ({
    data: mockData || data,
    isInitialLoading,
  });
};

export default useDataNodes;
