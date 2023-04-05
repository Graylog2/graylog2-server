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

const DataNodesOverview = () => {
  const { data: dataNodes, isFetching: isFetchingDataNodes, error: dataNodesFetchError } = useDataNodes();

  return (
    <>
      <p>
        These are the data nodes which are currently registered. The list is constantly updated. {isFetchingDataNodes
        && <Spinner text="" />}
      </p>

      <Space h="sm" />
      {!!dataNodes.length && (
        <Table verticalSpacing="xxs" fontSize="md">
          <thead>
            <tr>
              <th>Id</th>
              <th>Transport Address</th>
              <th>Is Secured</th>
            </tr>
          </thead>
          <tbody>
            {dataNodes.map((dataNode) => (
              <tr key={dataNode.id}>
                <td>{dataNode.id}</td>
                <td>{dataNode.transportAddress}</td>
                <td>{dataNode.isSecured ? 'yes' : 'no'}</td>
              </tr>
            ))}
          </tbody>
        </Table>
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
