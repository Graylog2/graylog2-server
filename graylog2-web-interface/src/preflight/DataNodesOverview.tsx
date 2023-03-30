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
import { Title, Table } from '@mantine/core';

const availableDataNodes = [
  { id: "data-node-id-1", transportAddress: 'transport.address1', isSecured: false },
  { id: "data-node-id-2", transportAddress: 'transport.address2', isSecured: false },
  { id: "data-node-id-3", transportAddress: 'transport.address3', isSecured: false }
]

const DataNodesOverview = () => (
  <>
    <Title order={3}>Available Data Nodes</Title>
    <p>
      These are the data nodes which are currently registered.<br />
      The list is constantly updated.
    </p>

    <Table>
      <thead>
      <tr>
        <th>Id</th>
        <th>Transport Address</th>
        <th>Is Secured</th>
      </tr>
      </thead>
      <tbody>
      {availableDataNodes.map((dataNode) => (
        <tr key={dataNode.id}>
          <td>{dataNode.id}</td>
          <td>{dataNode.transportAddress}</td>
          <td>{dataNode.isSecured ? 'yes' : 'no'}</td>
        </tr>
      ))}
      </tbody>
    </Table>
  </>
);
export default DataNodesOverview;
