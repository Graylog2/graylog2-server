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

import { Button, Label } from 'components/bootstrap';
import HelpPopoverButton from 'components/common/HelpPopoverButton';
import Title from 'components/common/Title';
import { startShardReplication, stopShardReplication } from 'components/datanode/hooks/useDataNodeUpgradeStatus';
import type { DatanodeUpgradeStatus } from 'components/datanode/hooks/useDataNodeUpgradeStatus';

const ServerVersion = styled.dl(
  ({ theme }) => css`
    color: ${theme.colors.gray[60]};
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

const ShardReplicationContainer = styled.div`
  display: flex;
  height: 20px;
  align-items: center;
`;

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
};

const clusterStateHelp = (
  <>
    <p>How does my cluster change state during the rolling upgrade?</p>
    <p>
      RED - if you are using indices with no replication and upgrade the node hosting the shards of these indices, the
      cluster will go to a red state and no data will be ingested into or searchable from these indices.
    </p>
    <p>
      YELLOW - after starting the upgrade of a node, shard allocation will be set to no replication to allow OpenSearch
      to use only the available shards.
    </p>
    <p>
      After a node has been upgraded and you click on <em>Confirm Upgrade</em>, shard replication will be re-enabled and
      all shards that were unavailable due to the node being upgraded will be re-allocated and the cluster will return
      to a GREEN state.
    </p>
  </>
);

const shardReplicationHelp = (shardReplicationEnabled: boolean) => (
  <>
    <p>
      After you click on{' '}
      <em>
        <b>Start Upgrade Process</b>
      </em>{' '}
      of a node, shard allocation will be set to no replication to allow OpenSearch to use only the available shards.
    </p>
    <p>
      After a node has been upgraded and you click on{' '}
      <em>
        <b>Confirm Upgrade</b>
      </em>
      , shard replication will be re-enabled and all shards that were unavailable due to the node being upgraded will be
      re-allocated.
    </p>
    <br />
    <Button
      onClick={shardReplicationEnabled ? stopShardReplication : startShardReplication}
      bsStyle="warning"
      bsSize="xsmall">
      Force {shardReplicationEnabled ? 'Disabled' : 'Enabled'}
    </Button>
  </>
);

type Props = {
  data: DatanodeUpgradeStatus | undefined;
  numberOfNodes: number;
  showShardReplication: boolean;
};

const ClusterHealthInfo = ({ data, numberOfNodes, showShardReplication }: Props) => (
  <>
    <Title order={3}>
      <Label bsStyle={getClusterHealthStyle(data?.cluster_state?.status)} bsSize="large">
        {data?.cluster_state?.cluster_name}: {data?.cluster_state?.status}
      </Label>
      &nbsp;
      <HelpPopoverButton helpText={clusterStateHelp} />
    </Title>
    <StyledHorizontalDl>
      <dt>Server Version:</dt>
      <ServerVersion>
        <b>{data?.server_version?.version || ''}</b>
      </ServerVersion>
      {showShardReplication && (
        <>
          <dt>Shard Replication:</dt>
          <dd>
            <ShardReplicationContainer>
              {data?.shard_replication_enabled ? (
                <Label bsStyle="success">Enabled</Label>
              ) : (
                <Label bsStyle="warning">Disabled</Label>
              )}
              &nbsp;
              <HelpPopoverButton helpText={shardReplicationHelp(!!data?.shard_replication_enabled)} />
            </ShardReplicationContainer>
          </dd>
        </>
      )}
      <dt>Cluster Manager:</dt>
      <dd>{data?.cluster_state?.manager_node?.name}</dd>
      <dt>Number of Nodes:</dt>
      <dd>
        {numberOfNodes} ({data?.outdated_nodes?.length || 0} outdated, {data?.up_to_date_nodes?.length || 0} upgraded)
      </dd>
      <dt>Number of Shards:</dt>
      <dd>
        {data?.cluster_state?.active_shards || 0} active,&nbsp;
        {data?.cluster_state?.initializing_shards || 0} initializing,&nbsp;
        {data?.cluster_state?.relocating_shards || 0} relocating,&nbsp;
        {data?.cluster_state?.unassigned_shards || 0} unassigned
      </dd>
    </StyledHorizontalDl>
  </>
);

export default ClusterHealthInfo;
