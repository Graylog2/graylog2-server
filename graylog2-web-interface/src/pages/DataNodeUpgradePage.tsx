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

import { Row, Col, Button } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import useDataNodeUpgradeStatus from 'components/datanode/hooks/useDataNodeUpgradeStatus';
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

const NodeListItem = styled.div(
  ({ theme }) => css`
    margin: ${theme.spacings.sm} 0;
  `,
);

const DataNodeUpgradePage = () => {
  const { data, isInitialLoading } = useDataNodeUpgradeStatus();

  const confirmUpgradeButton = (
    <Button onClick={() => {}} bsSize="xsmall" bsStyle="success">
      Confirm Upgrade here
    </Button>
  );

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
          <h2>{data?.cluster_state?.cluster_name} {data?.cluster_healthy ? 'GREEN' : 'RED'}</h2>
          <StyledHorizontalDl>
            <dt>Shard Replication:</dt>
            <dd>{data?.shard_replication_enabled ? 'Enabled' : 'Disabled'}</dd>
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
              {data?.outdated_nodes?.map((outdated_node) => (
                <NodeListItem>{outdated_node?.hostname}</NodeListItem>
              ))}
            </Col>
            <Col xs={6}>
              <h3>Updated Nodes</h3>
              {data?.up_to_date_nodes?.map((updated_node) => (
                <NodeListItem>{updated_node?.hostname}</NodeListItem>
              ))}
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
