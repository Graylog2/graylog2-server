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
import { Space } from '@mantine/core';

import Spinner from 'components/common/Spinner';
import useDataNodes from 'preflight/hooks/useDataNodes';
import { Alert, Table } from 'preflight/components/common';
import Timestamp from 'components/common/Timestamp';

const DataNodesOverview = () => {
  const {
    data: dataNodes,
    isFetching: isFetchingDataNodes,
    error: dataNodesFetchError,
    isInitialLoading: isInitialLoadingDataNodes,
  } = useDataNodes();

  return (
    <>
      <p>
        These are the data nodes which are currently registered. The list is constantly updated.
        {isFetchingDataNodes && <Spinner text="" />}
      </p>

      {!!dataNodes.length && (
        <>
          <Space h="sm" />
          <Table verticalSpacing="xxs" fontSize="md">
            <thead>
              <tr>
                <th>Id</th>
                <th>Hostname</th>
                <th>Transport Address</th>
                <th>Node Id</th>
                <th>Short Node Id</th>
                <th>Is Leader</th>
                <th>Is Master</th>
                <th>Last Seen</th>
              </tr>
            </thead>
            <tbody>
              {dataNodes.map(({
                id,
                hostname,
                transport_address,
                node_id,
                short_node_id,
                is_leader,
                is_master,
                last_seen,
              }) => (
                <tr key={id}>
                  <td>{id}</td>
                  <td>{hostname}</td>
                  <td>{transport_address}</td>
                  <td>{node_id}</td>
                  <td>{short_node_id}</td>
                  <td>{is_leader ? 'yes' : 'no'}</td>
                  <td>{is_master ? 'yes' : 'no'}</td>
                  <td><Timestamp dateTime={last_seen} /></td>
                </tr>
              ))}
            </tbody>
          </Table>
        </>
      )}
      {(!dataNodes.length && !isInitialLoadingDataNodes) && (
        <Alert type="info">
          No data nodes have been found.
        </Alert>
      )}
      {dataNodesFetchError && (
        <Alert type="danger">
          There was an error fetching the data nodes: {dataNodesFetchError.message}
        </Alert>
      )}
    </>
  );
};

export default DataNodesOverview;
