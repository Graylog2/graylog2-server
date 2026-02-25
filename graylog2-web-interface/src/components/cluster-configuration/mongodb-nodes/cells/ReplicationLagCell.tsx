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

import NumberUtils from 'util/NumberUtils';

import { MetricPlaceholder, MetricsColumn, MetricsRow, StyledLabel } from '../../shared-components/NodeMetricsLayout';
import { MongodbRole, type MongodbRoleType } from '../fetchClusterMongodbNodes';

type Props = {
  replicationLag: number | undefined | null;
  role: string | undefined | null;
  warningThreshold: number;
  dangerThreshold: number;
};

const MS_IN_SECOND = 1000;
const SECONDS_IN_MINUTE = 60;
const MS_IN_MINUTE = SECONDS_IN_MINUTE * MS_IN_SECOND;
const NO_REPLICATION_LAG_ROLES: Set<MongodbRoleType> = new Set([
  MongodbRole.PRIMARY,
  MongodbRole.STANDALONE,
  MongodbRole.ARBITER,
]);

export const formatReplicationLagMs = (replicationLagMs: number): string => {
  if (replicationLagMs < MS_IN_SECOND) {
    return `${NumberUtils.formatNumber(replicationLagMs)} ms`;
  }

  if (replicationLagMs < MS_IN_MINUTE) {
    return `${NumberUtils.formatNumber(replicationLagMs / MS_IN_SECOND)} s`;
  }

  const wholeMinutes = Math.floor(replicationLagMs / MS_IN_MINUTE);
  const remainingSeconds = (replicationLagMs % MS_IN_MINUTE) / MS_IN_SECOND;

  if (remainingSeconds === 0) {
    return `${NumberUtils.formatNumber(wholeMinutes)} min`;
  }

  return `${NumberUtils.formatNumber(wholeMinutes)} min ${NumberUtils.formatNumber(remainingSeconds)} s`;
};

const ReplicationLagCell = ({ replicationLag, role, warningThreshold, dangerThreshold }: Props) => {
  const upperRole = role?.toUpperCase();

  if (upperRole && NO_REPLICATION_LAG_ROLES.has(upperRole as MongodbRoleType)) {
    return (
      <MetricsColumn>
        <MetricsRow>
          <span>-</span>
        </MetricsRow>
      </MetricsColumn>
    );
  }

  if (replicationLag == null) {
    return <MetricPlaceholder />;
  }

  const formatted = formatReplicationLagMs(replicationLag);
  const exactValueTitle = `${NumberUtils.formatNumber(replicationLag)} ms`;
  const exceedsDanger = replicationLag >= dangerThreshold;
  const exceedsWarning = !exceedsDanger && replicationLag >= warningThreshold;

  if (!exceedsDanger && !exceedsWarning) {
    return (
      <MetricsColumn>
        <MetricsRow>
          <span title={exactValueTitle}>{formatted}</span>
        </MetricsRow>
      </MetricsColumn>
    );
  }

  return (
    <MetricsColumn>
      <MetricsRow>
        <StyledLabel bsStyle={exceedsDanger ? 'danger' : 'warning'} bsSize="xs" title={exactValueTitle}>
          {formatted}
        </StyledLabel>
      </MetricsRow>
    </MetricsColumn>
  );
};

export default ReplicationLagCell;
