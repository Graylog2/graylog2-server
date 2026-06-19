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
import React from 'react';
import styled from 'styled-components';

import { Col, Label, Row, Table } from 'components/bootstrap';
import { Icon, ProgressBar } from 'components/common';
import type { DataNodeInformation } from 'components/datanode/hooks/useDataNodeUpgradeStatus';

type Props = {
  pendingNodes: DataNodeInformation[];
  upgradedNodes: DataNodeInformation[];
  currentProgress?: number;
};

const NodeProgressBar = styled(ProgressBar)`
  display: inline-flex;
  width: 140px;
  margin-bottom: 0;
  vertical-align: middle;
`;

const NodeIdentityCell = ({ node }: { node: DataNodeInformation }) => (
  <td>
    <div>
      {node?.hostname}&nbsp;
      <Label bsStyle={node?.data_node_status === 'AVAILABLE' ? 'success' : 'warning'} bsSize="xs">
        {node?.data_node_status}
      </Label>
      &nbsp;
      {node?.manager_node && (
        <Label bsStyle="info" bsSize="xs">
          manager
        </Label>
      )}
    </div>
    <div>
      <i>{node?.ip}</i>
    </div>
  </td>
);

const UpgradingStatus = ({ progress }: { progress?: number }) =>
  typeof progress === 'number' ? (
    <NodeProgressBar
      bars={[{ value: progress, label: `${progress}%`, bsStyle: 'warning', animated: true, striped: true }]}
    />
  ) : (
    <Label bsStyle="warning" bsSize="xs">
      Upgrading…
    </Label>
  );

const OpenSearchRollingUpgradeNodes = ({ pendingNodes, upgradedNodes, currentProgress = undefined }: Props) => (
  <>
    <br />
    <Row>
      <Col sm={6}>
        <h3>Pending OpenSearch Upgrade</h3>
        <br />
        <Table>
          <tbody>
            {pendingNodes?.map((node, index) => (
              <tr key={node?.hostname}>
                <NodeIdentityCell node={node} />
                <td>
                  <i>{node?.opensearch_version}</i>
                </td>
                <td align="right">
                  {index === 0 ? (
                    <UpgradingStatus progress={currentProgress} />
                  ) : (
                    <Label bsStyle="default" bsSize="xs">
                      Pending
                    </Label>
                  )}
                </td>
              </tr>
            ))}
            {!pendingNodes?.length && (
              <tr>
                <td>No pending nodes.</td>
              </tr>
            )}
          </tbody>
        </Table>
      </Col>
      <Col sm={6}>
        <h3>Upgraded OpenSearch</h3>
        <br />
        <Table>
          <tbody>
            {upgradedNodes?.map((node) => (
              <tr key={node?.hostname}>
                <NodeIdentityCell node={node} />
                <td>
                  <i>{node?.opensearch_version}</i>
                </td>
                <td align="right">
                  <Label bsStyle="success" bsSize="xs">
                    Upgraded <Icon name="check" />
                  </Label>
                </td>
              </tr>
            ))}
            {!upgradedNodes?.length && (
              <tr>
                <td>No upgraded nodes yet.</td>
              </tr>
            )}
          </tbody>
        </Table>
      </Col>
    </Row>
  </>
);

export default OpenSearchRollingUpgradeNodes;
