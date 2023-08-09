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
import * as React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Space } from '@mantine/core';
import styled from 'styled-components';

import { qualifyUrl } from 'util/URLUtils';
import { fetchPeriodically } from 'logic/rest/FetchProvider';
import type { DataNode } from 'preflight/types';
import UserNotification from 'util/UserNotification';
import { Spinner } from 'components/common';
import { Alert, Badge, ListGroup, ListGroupItem } from 'components/bootstrap';

const StyledList = styled(ListGroup)`
  max-width: 700px;
`;

const DataNodeInfos = styled.div`
  display: flex;
  align-items: center;
  gap: 3px;
`;

const NodeId = styled(Badge)`
  margin-right: 3px;
`;

export const fetchDataNodes = () => {
  const url = qualifyUrl('certrenewal');

  return fetchPeriodically<Array<DataNode>>('GET', url);
};

const useDataNodes = () => {
  const { data, isInitialLoading } = useQuery({
    queryKey: ['data-nodes', 'overview'],
    queryFn: fetchDataNodes,
    onError: (errorThrown) => {
      UserNotification.error(`Loading data nodes failed with status: ${errorThrown}`,
        'Could not load streams');
    },
    keepPreviousData: true,
    refetchInterval: 3000,

  });

  return ({
    data,
    isInitialLoading,
  });
};

const DataNodesCertificateRenewal = () => {
  const { data: dataNodes, isInitialLoading: isInitialLoadingDataNodes } = useDataNodes();

  return (
    <div>
      <h2>Graylog Data Nodes Certificate Renewal</h2>
      <p>
        Here you can manually trigger the certificate renewal for Graylog data nodes.
      </p>

      {!!dataNodes?.length && (
        <StyledList>
            {dataNodes.map(({
              hostname,
              transport_address,
              short_node_id,
            }) => (
              <ListGroupItem key={short_node_id}>
                <DataNodeInfos>
                  <NodeId title="Short node id">{short_node_id}</NodeId>
                  <span title="Transport address">{transport_address}</span>{' – '}
                  <span title="Hostname">{hostname}</span>
                </DataNodeInfos>
              </ListGroupItem>
            ))}
        </StyledList>
      )}

      {isInitialLoadingDataNodes && <Spinner />}
      {(!dataNodes?.length && !isInitialLoadingDataNodes) && (
        <Alert>
          No data nodes have been found.
        </Alert>
      )}
    </div>
  );
};

export default DataNodesCertificateRenewal;
