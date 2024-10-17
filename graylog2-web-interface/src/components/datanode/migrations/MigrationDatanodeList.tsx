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

import { Icon, Spinner, Timestamp } from 'components/common';
import { Alert, Table } from 'components/bootstrap';
import { DocumentationLink } from 'components/support';
import useDataNodes from 'components/datanode/hooks/useDataNodes';

type Props = {
  showProvisioningState?: boolean
}

const StyledIcon = styled(Icon)`
  margin-right: 0.5em;
`;

const MigrationDatanodeList = ({ showProvisioningState = true }: Props) => {
  const { data: dataNodes, isInitialLoading } = useDataNodes();

  if (isInitialLoading) {
    return <Spinner text="Loading Data Nodes" />;
  }

  return (
    <div>
      {(!dataNodes || dataNodes?.list.length === 0) ? (
        <>
          <p><StyledIcon name="info" />There are no Data Nodes found.</p>
          <Alert bsStyle="warning" title="No Data Nodes found">
            Please start at least a Data Node to continue the migration process. You can find more information on how to start a Data Nodes in our <DocumentationLink page="graylog-data-node" text="documentation" />.
          </Alert>
          <p><Spinner text="Looking for Data Nodes..." /></p>
        </>
      ) : (
        <>
          <h4>Data Nodes found: {dataNodes?.list.length}</h4>
          {dataNodes.list.find((datanode) => !datanode.version_compatible) && (
            <Alert bsStyle="warning" title="Incompatible Data Nodes found">
              There are Data Nodes running with versions incompatible to your current Graylog version.
              Please make sure to use the same version for both Graylog and Data Node.
            </Alert>
          )}
          <br />
          <Table bordered condensed striped hover>
            <thead>
              <tr>
                <th>Hostname</th>
                <th>Transport address</th>
                <th>Status</th>
                <th>Certificate valid until</th>
                <th>Version</th>
              </tr>
            </thead>
            <tbody>
              {dataNodes.list.map((datanode) => (
                <tr key={datanode.id}>
                  <td>{datanode.hostname}</td>
                  <td>{datanode.transport_address}</td>
                  <td>{showProvisioningState ? datanode.status : datanode.data_node_status}</td>
                  <td>{datanode.cert_valid_until ? <Timestamp dateTime={datanode.cert_valid_until} /> : 'No certificate'}</td>
                  <td>
                    {!datanode.version_compatible && (
                      <Icon name="warning"
                            title="This version is incompatible with your current Graylog version." />
                    )}
                    {datanode.datanode_version}
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        </>
      )}
    </div>
  );
};

export default MigrationDatanodeList;
