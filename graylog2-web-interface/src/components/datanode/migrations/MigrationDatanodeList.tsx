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
import styled from 'styled-components';

import type { DataNodeResponse } from 'components/datanode/hooks/useDataNodes';
import { Icon, Spinner } from 'components/common';
import { Alert, Table } from 'components/bootstrap';
import { DocumentationLink } from 'components/support';

type Props = {
  dataNodes: DataNodeResponse,
}
const StyledIcon = styled(Icon)`
  margin-right: 0.5em;
`;

const MigrationDatanodeList = ({ dataNodes } : Props) => (
  <div>
    {(!dataNodes || dataNodes?.elements.length === 0) ? (
      <>
        <p><StyledIcon name="info-circle" />There are no data nodes found.</p>
        <Alert bsStyle="warning" title="No data nodes found">
          Please start at least a datanode to continue the migration process. You can find more information on how to start a data nodes in our <DocumentationLink page="graylog-data-node" text="documentation" />.
        </Alert>
        <p><Spinner text="Looking for data nodes..." /></p>
      </>
    ) : (
      <>
        <h4>Datanodes found:</h4>
        <br />
        <Table bordered condensed striped hover>
          <thead>
            <tr>
              <th>Hostname</th>
              <th>Transport address</th>
              <th>status</th>
            </tr>
          </thead>
          <tbody>
            {dataNodes.elements.map((datanode) => (
              <tr key={datanode.id}>
                <td>{datanode.hostname}</td>
                <td>{datanode.transport_address}</td>
                <td>{datanode.status}</td>
              </tr>
            ))}
          </tbody>
        </Table>
      </>
    )}
  </div>
);

export default MigrationDatanodeList;
