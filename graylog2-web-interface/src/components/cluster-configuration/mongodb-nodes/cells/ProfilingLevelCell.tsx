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

import { MetricsColumn, MetricsRow, StyledLabel } from '../../shared-components/NodeMetricsLayout';
import { MongodbProfilingLevel, type MongodbProfilingLevelType } from '../fetchClusterMongodbNodes';

const LEVEL_LABELS: Record<MongodbProfilingLevelType, { label: string; style: string }> = {
  [MongodbProfilingLevel.OFF]: { label: 'Off', style: 'default' },
  [MongodbProfilingLevel.SLOW_OPS]: { label: 'Slow Ops', style: 'info' },
  [MongodbProfilingLevel.ALL]: { label: 'All', style: 'warning' },
};

type Props = {
  profilingLevel: MongodbProfilingLevelType | undefined | null;
};

const ProfilingLevelCell = ({ profilingLevel }: Props) => {
  if (profilingLevel == null) {
    return null;
  }

  const levelInfo = LEVEL_LABELS[profilingLevel];
  const resolvedLevelInfo = levelInfo ?? { label: `Unknown (${profilingLevel})`, style: 'default' };

  return (
    <MetricsColumn>
      <MetricsRow>
        <StyledLabel bsStyle={resolvedLevelInfo.style} bsSize="xs">
          {resolvedLevelInfo.label}
        </StyledLabel>
      </MetricsRow>
    </MetricsColumn>
  );
};

export default ProfilingLevelCell;
