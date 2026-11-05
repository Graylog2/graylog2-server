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
import { useTranslation } from 'react-i18next';

import type { ColorVariant } from 'theme/sawmill';
import { Label } from 'components/bootstrap';

import { MetricsColumn, MetricsRow } from '../../shared-components/NodeMetricsLayout';
import { MongodbProfilingLevel, type MongodbProfilingLevelType } from '../fetchClusterMongodbNodes';

const LEVEL_LABELS: Record<MongodbProfilingLevelType, { labelKey: string; style: ColorVariant }> = {
  [MongodbProfilingLevel.OFF]: { labelKey: 'mongodb_nodes.profiling_levels.off', style: 'default' },
  [MongodbProfilingLevel.SLOW_OPS]: { labelKey: 'mongodb_nodes.profiling_levels.slow_ops', style: 'info' },
  [MongodbProfilingLevel.ALL]: { labelKey: 'mongodb_nodes.profiling_levels.all', style: 'warning' },
};

type Props = {
  profilingLevel: MongodbProfilingLevelType | undefined | null;
};

const ProfilingLevelCell = ({ profilingLevel }: Props) => {
  const { t } = useTranslation('system');

  if (profilingLevel == null) {
    return null;
  }

  const levelInfo = LEVEL_LABELS[profilingLevel];
  const resolvedLevelInfo: { label: string; style: ColorVariant } = levelInfo
    ? { label: t(levelInfo.labelKey), style: levelInfo.style }
    : { label: t('mongodb_nodes.profiling_levels.unknown', { level: profilingLevel }), style: 'default' };

  return (
    <MetricsColumn>
      <MetricsRow>
        <Label bsStyle={resolvedLevelInfo.style} bsSize="xs">
          {resolvedLevelInfo.label}
        </Label>
      </MetricsRow>
    </MetricsColumn>
  );
};

export default ProfilingLevelCell;
