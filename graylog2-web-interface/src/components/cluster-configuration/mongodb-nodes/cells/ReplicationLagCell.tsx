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

type Props = {
  replicationLag: number | undefined | null;
  role: string | undefined | null;
  warningThreshold: number;
  dangerThreshold: number;
};

const ReplicationLagCell = ({ replicationLag, role, warningThreshold, dangerThreshold }: Props) => {
  if (role?.toUpperCase() === 'PRIMARY') {
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

  const formatted = `${NumberUtils.formatNumber(replicationLag)} ms`;
  const exceedsDanger = replicationLag >= dangerThreshold;
  const exceedsWarning = !exceedsDanger && replicationLag >= warningThreshold;

  if (!exceedsDanger && !exceedsWarning) {
    return (
      <MetricsColumn>
        <MetricsRow>
          <span>{formatted}</span>
        </MetricsRow>
      </MetricsColumn>
    );
  }

  return (
    <MetricsColumn>
      <MetricsRow>
        <StyledLabel bsStyle={exceedsDanger ? 'danger' : 'warning'} bsSize="xs">
          {formatted}
        </StyledLabel>
      </MetricsRow>
    </MetricsColumn>
  );
};

export default ReplicationLagCell;
