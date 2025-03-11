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
import React, { useState } from 'react';
import styled, { css } from 'styled-components';

import { Row, Col, Button, Table, Label, Panel, SegmentedControl } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner, Icon } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import useDataNodeUpgradeStatus, { getNodeToUpgrade, saveNodeToUpgrade, startShardReplication, stopShardReplication } from 'components/datanode/hooks/useDataNodeUpgradeStatus';
import type { DataNodeInformation } from 'components/datanode/hooks/useDataNodeUpgradeStatus';
import ClusterConfigurationPageNavigation from 'components/cluster-configuration/ClusterConfigurationPageNavigation';

const StyledSegmentedControl = styled(SegmentedControl)(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.sm};
  `,
);

const StyledPanel = styled(Panel)<{ bsStyle: string }>(
  ({ bsStyle = 'default', theme }) => css`
    &.panel {
      background-color: ${theme.colors.global.contentBackground};

      .panel-heading {
        color: ${theme.colors.variant.darker[bsStyle]};
      }
    }
  `,
);

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

type DataNodeUpgradeMethodType = 'cluster-restart'|'rolling-upgrade';

const UpgradeMethodSegments: Array<{ value: DataNodeUpgradeMethodType; label: string }> = [
  { value: 'cluster-restart', label: 'Cluster Restart' },
  { value: 'rolling-upgrade', label: 'Rolling Upgrade' },
];

const DataNodeUpgradePage = () => {
  const { data, isInitialLoading } = useDataNodeUpgradeStatus();
  const [upgradeMethod, setUpgradeMethod] = useState<DataNodeUpgradeMethodType>('cluster-restart');

  const confirmUpgradeButton = (
    <Button onClick={startShardReplication} bsSize="sm" bsStyle="primary">
      Confirm Upgrade Here
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

  const upgradeNode = async (node: DataNodeInformation) => {
    await stopShardReplication();
    saveNodeToUpgrade(node?.hostname);
  }

  const nodeInProgress = getNodeToUpgrade();

  const numberOfNodes = (data?.outdated_nodes?.length || 0) + (data?.up_to_date_nodes?.length || 0);

  const showRollingUpgrade = numberOfNodes > 2;

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
      {isInitialLoading ? <Spinner /> : (
        <Row className="content">
          <Col xs={12}>
            <Row>
              <Col xs={6}>
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
                  <dd>{numberOfNodes} ({data?.outdated_nodes?.length || 0} outdated, {data?.up_to_date_nodes?.length || 0} upgraded)</dd> 
                  <dt>Number of Shards:</dt>
                  <dd>{data?.cluster_state?.active_shards || 0} ({data?.cluster_state?.unassigned_shards || 0} unassigned)</dd>
                </StyledHorizontalDl>
              </Col>
              <Col xs={6}>
                <StyledPanel bsStyle="info">
                  <Panel.Heading>
                    <Panel.Title componentClass="h3">
                      <Icon name="info" /> Methods for Data Node Upgrade
                    </Panel.Title>
                  </Panel.Heading>
                  <Panel.Body>
                    <StyledSegmentedControl data={UpgradeMethodSegments} value={upgradeMethod} onChange={(value: DataNodeUpgradeMethodType) => setUpgradeMethod(value)} />
                    {(upgradeMethod === 'cluster-restart') && (
                      <>
                        <p>
                          When using the cluster restart method, you will upgrade all Data Nodes at once. During this time, 
                          messages will be buffered in the journal and processed as the Data Node cluster comes back online, 
                          leading to no data loss provided your journal size is configured for the message volume which is expected during the Data Node downtime.               
                        </p>
                        <p>
                          If you are running a Data Node cluster with less than three nodes, the cluster restart method is the only method available.
                        </p>
                        <p>
                          If you are running a Data Node cluster with three or more nodes, you can choose to use the cluster restart method after consideration of your journal size and your message throughput.
                        </p>
                        <p>
                          To upgrade your Data Nodes, please follow the instructions in the documentation.
                        </p>
                      </>
                    )}
                    {(upgradeMethod === 'rolling-upgrade') && (
                      <p>
                        Rolling upgrades can be performed on a running Data Node cluster with virtually no downtime. 
                        Data Nodes are individually stopped and upgraded in place. Alternatively, Data Nodes can be stopped and replaced, one at a time, 
                        by hosts running the new version. During this process you can continue to index and query data in your cluster.               
                      </p>
                    )}
                  </Panel.Body>
                </StyledPanel>
              </Col>
            </Row>
            <br />
          </Col>
          {showRollingUpgrade && (
            <Col xs={12}>
              <Row>
                <Col sm={6}>
                  <h3>Outdated Nodes</h3>
                  <br />
                  <Table>
                    <tbody>
                      {data?.outdated_nodes?.map((outdated_node) => (
                        <tr key={outdated_node?.hostname}>
                          <td>
                            <div>
                              {outdated_node?.hostname}&nbsp;
                              <Label bsStyle={outdated_node?.data_node_status === 'AVAILABLE' ? 'success' : 'warning'} bsSize="xs">
                                {outdated_node?.data_node_status}
                              </Label>&nbsp;
                              {outdated_node?.manager_node && (<Label bsStyle="info" bsSize="xs">manager</Label>)}
                            </div>
                            <div>
                              <i>{outdated_node?.ip}</i>
                            </div>
                          </td>
                          <td><i>{outdated_node?.datanode_version}</i></td>
                          <td align="right">
                            <Button onClick={() => upgradeNode(outdated_node)} disabled={!outdated_node?.upgrade_possible} bsSize="sm" bsStyle="primary">
                              Upgrade
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
                <Col sm={6}>
                  <h3>Upgraded Nodes</h3>
                  <br />
                  <Table>
                    <tbody>
                      {data?.up_to_date_nodes?.map((upgraded_node) => (
                        <tr key={upgraded_node?.hostname}>
                          <td>
                            <div>
                              {upgraded_node?.hostname}&nbsp;
                              <Label bsStyle={upgraded_node?.data_node_status === 'AVAILABLE' ? 'success' : 'warning'} bsSize="xs">
                                {upgraded_node?.data_node_status}
                              </Label>&nbsp;
                              {upgraded_node?.manager_node && (<Label bsStyle="info" bsSize="xs">manager</Label>)}
                            </div>
                            <div>
                              <i>{upgraded_node?.ip}</i>
                            </div>
                          </td>
                          <td><i>{upgraded_node?.datanode_version}</i></td>
                          <td align="right">
                            <Label bsStyle="success" bsSize="xs">Upgraded <Icon name="check" /></Label>
                          </td>
                        </tr>
                      ))}
                      {!data?.up_to_date_nodes?.length && (
                        <tr>
                          <td>No upgraded nodes found.</td>
                        </tr>
                      )}
                    </tbody>
                  </Table>
                </Col>
              </Row>
            </Col>
          )}
          {nodeInProgress && (
            <Col xs={12}>
              <br />
              Once you are done upgrading <b>{nodeInProgress}</b>, wait until it reconnects and apears in the <b>Upgraded Nodes</b> panel,
              then {confirmUpgradeButton} and continue with next node.
            </Col>
          )}
        </Row>
      )}
    </DocumentTitle>
  );
};

export default DataNodeUpgradePage;
