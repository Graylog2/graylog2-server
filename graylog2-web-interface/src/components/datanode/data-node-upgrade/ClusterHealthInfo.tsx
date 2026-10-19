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
import { Trans, useTranslation } from 'react-i18next';

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

const ClusterStateHelp = () => {
  const { t } = useTranslation('datanode');

  return (
    <>
      <p>{t('cluster_health.state_help.question')}</p>
      <p>{t('cluster_health.state_help.red')}</p>
      <p>{t('cluster_health.state_help.yellow')}</p>
      <p>
        <Trans t={t} i18nKey="cluster_health.state_help.green" components={{ emphasis: <em /> }} />
      </p>
    </>
  );
};

const ShardReplicationHelp = ({ shardReplicationEnabled }: { shardReplicationEnabled: boolean }) => {
  const { t } = useTranslation('datanode');

  return (
    <>
      <p>
        <Trans
          t={t}
          i18nKey="cluster_health.shard_replication_help.start"
          components={{ emphasis: <em />, bold: <b /> }}
        />
      </p>
      <p>
        <Trans
          t={t}
          i18nKey="cluster_health.shard_replication_help.confirm"
          components={{ emphasis: <em />, bold: <b /> }}
        />
      </p>
      <br />
      <Button
        onClick={shardReplicationEnabled ? stopShardReplication : startShardReplication}
        bsStyle="warning"
        bsSize="xsmall">
        {shardReplicationEnabled
          ? t('cluster_health.shard_replication_help.force_disabled')
          : t('cluster_health.shard_replication_help.force_enabled')}
      </Button>
    </>
  );
};

type Props = {
  data: DatanodeUpgradeStatus | undefined;
  numberOfNodes: number;
  showShardReplication: boolean;
};

const ClusterHealthInfo = ({ data, numberOfNodes, showShardReplication }: Props) => {
  const { t } = useTranslation('datanode');

  return (
    <>
      <Title order={3}>
        <Label bsStyle={getClusterHealthStyle(data?.cluster_state?.status)} bsSize="large">
          {data?.cluster_state?.cluster_name}: {data?.cluster_state?.status}
        </Label>
        &nbsp;
        <HelpPopoverButton helpText={<ClusterStateHelp />} />
      </Title>
      <StyledHorizontalDl>
        <dt>{t('cluster_health.server_version')}</dt>
        <ServerVersion>
          <b>{data?.server_version?.version || ''}</b>
        </ServerVersion>
        {showShardReplication && (
          <>
            <dt>{t('cluster_health.shard_replication')}</dt>
            <dd>
              <ShardReplicationContainer>
                {data?.shard_replication_enabled ? (
                  <Label bsStyle="success">{t('cluster_health.enabled')}</Label>
                ) : (
                  <Label bsStyle="warning">{t('cluster_health.disabled')}</Label>
                )}
                &nbsp;
                <HelpPopoverButton
                  helpText={<ShardReplicationHelp shardReplicationEnabled={!!data?.shard_replication_enabled} />}
                />
              </ShardReplicationContainer>
            </dd>
          </>
        )}
        <dt>{t('cluster_health.cluster_manager')}</dt>
        <dd>{data?.cluster_state?.manager_node?.name}</dd>
        <dt>{t('cluster_health.number_of_nodes')}</dt>
        <dd>
          {t('cluster_health.nodes_summary', {
            total: numberOfNodes,
            outdated: data?.outdated_nodes?.length || 0,
            upgraded: data?.up_to_date_nodes?.length || 0,
          })}
        </dd>
        <dt>{t('cluster_health.number_of_shards')}</dt>
        <dd>
          {t('cluster_health.shards_summary', {
            active: data?.cluster_state?.active_shards || 0,
            initializing: data?.cluster_state?.initializing_shards || 0,
            relocating: data?.cluster_state?.relocating_shards || 0,
            unassigned: data?.cluster_state?.unassigned_shards || 0,
          })}
        </dd>
      </StyledHorizontalDl>
    </>
  );
};

export default ClusterHealthInfo;
