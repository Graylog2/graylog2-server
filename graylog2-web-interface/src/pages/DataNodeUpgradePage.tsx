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
import styled, { css } from 'styled-components';

import { Row, Col, Button, Table, Label } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner, Icon } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import useDataNodeUpgradeStatus, { startShardReplication, stopShardReplication } from 'components/datanode/hooks/useDataNodeUpgradeStatus';
import ClusterConfigurationPageNavigation from 'components/cluster-configuration/ClusterConfigurationPageNavigation';

const StyledHorizontalDl = styled.dl(
  ({ theme }) => css`
    margin: ${theme.spacings.md} 0;

    > dt {
      clear: left;
      float: left;
      margin-bottom: ${theme.spacings.sm};
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      width: 160px;
    }

    > *:not(dt) {
      margin-bottom: ${theme.spacings.sm};
      margin-left: 140px;
    }
  `,
);

const Version = styled.b`
  font-size: 1rem;
`;

const DataNodeUpgradePage = () => {
  const { data, isInitialLoading } = useDataNodeUpgradeStatus();

  const confirmUpgradeButton = (
    <Button onClick={startShardReplication} bsSize="xsmall" bsStyle="primary">
      Confirm Upgrade here
    </Button>
  );

  const getClusterHealthStyle = (status: string) => {
    switch (status) {
      case 'GREEN':
        return 'success';
      case 'YELLOW':
        return 'warning';
      case 'RED':
        return 'danger';
      default:
        return 'info';
    }
  }

  return (
    <DocumentTitle title="Data Node Upgrade">
      <ClusterConfigurationPageNavigation />
      <PageHeader
        title="Data Node Upgrade"
        documentationLink={{
          title: 'Data Nodes documentation',
          path: DocsHelper.PAGES.GRAYLOG_DATA_NODE,
        }}>
        <span>
          Graylog Data Nodes offer a better integration with Graylog and simplify future updates. They allow you to index
          and search through all the messages in your Graylog message database.
        </span>
      </PageHeader>
      <Row className="content">
        <Col xs={12}>
          {isInitialLoading && <Spinner />}
          <h3>
            <Label bsStyle={getClusterHealthStyle(data?.cluster_state?.status)} bsSize="xs">
              {data?.cluster_state?.cluster_name}: {data?.cluster_state?.status}
            </Label>
          </h3>
          <StyledHorizontalDl>
            <dt>Shard Replication:</dt>
            <dd>
              {data?.shard_replication_enabled ? (
                <Label bsStyle="success" bsSize="xs">Enabled</Label>
              ) : (
                <Label bsStyle="warning" bsSize="xs">Disabled</Label>
              )}
            </dd>
            <dt>Cluster Manager:</dt>
            <dd>{data?.cluster_state?.manager_node?.name}</dd>
            <dt>Number of Nodes:</dt>
            <dd>{(data?.outdated_nodes?.length || 0) + (data?.up_to_date_nodes?.length || 0)} ({data?.outdated_nodes?.length || 0} outdated, {data?.up_to_date_nodes?.length || 0} updated)</dd> 
            <dt>Number of Shards:</dt>
            <dd>{data?.cluster_state?.active_shards || 0} ({data?.cluster_state?.unassigned_shards || 0} unassigned)</dd>
          </StyledHorizontalDl>
          <br />
        </Col>
        <Col xs={12}>
          <Row>
            <Col xs={6}>
              <h3>Outdated Nodes</h3>
              <br />
              <Table>
                <tbody>
                  {data?.outdated_nodes?.map((outdated_node) => (
                    <tr key={outdated_node?.hostname}>
                      <td>{outdated_node?.hostname} <i>{outdated_node?.ip}</i></td>
                      <td align="right">
                        <Button onClick={stopShardReplication} bsSize="xsmall" bsStyle="primary">
                          Update
                        </Button>
                      </td>
                    </tr>
                  ))}
                  {!data?.outdated_nodes?.length && (
                    <tr>
                      <td>No outdated nodes found.</td>
                    </tr>
                  )}
                </tbody>
              </Table>
            </Col>
            <Col xs={6}>
              <h3>Updated Nodes <Version>v{data?.server_version?.version}</Version></h3>
              <br />
              <Table>
                <tbody>
                  {data?.up_to_date_nodes?.map((updated_node) => (
                    <tr key={updated_node?.hostname}>
                      <td>{updated_node?.hostname} <i>{updated_node?.ip}</i></td>
                      <td align="right">
                        <Label bsStyle="success" bsSize="xs">Updated <Icon name="check" /></Label>
                      </td>
                    </tr>
                  ))}
                  {!data?.up_to_date_nodes?.length && (
                    <tr>
                      <td>No updated nodes found.</td>
                    </tr>
                  )}
                </tbody>
              </Table>
            </Col>
          </Row>
        </Col>
        <Col xs={12}>
          <br />
          You are upgrading <b>TODO NODE</b>, wait until it reconnects and apears in the <b>Updated Nodes</b> panel,
          then {confirmUpgradeButton} and continue with next node.
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default DataNodeUpgradePage;
