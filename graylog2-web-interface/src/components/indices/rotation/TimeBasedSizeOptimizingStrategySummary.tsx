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
import * as React from 'react';

import { Pluralize } from 'components/common';

import type { TimeBasedSizeOptimizingStrategyConfig } from './TimeBasedSizeOptimizingStrategyConfiguration';
import { durationToRoundedDays } from './TimeBasedSizeOptimizingStrategyConfiguration';

const TimeBasedSizeOptimizingStrategySummary = ({ config: { index_lifetime_max, index_lifetime_min } }: {config: TimeBasedSizeOptimizingStrategyConfig}) => {
  const minLifetimeAsDays = durationToRoundedDays(index_lifetime_min);
  const maxLifetimeAsDays = durationToRoundedDays(index_lifetime_max);

  return (
    <div>
      <dl>
        <dt>Index rotation strategy:</dt>
        <dd>Index Time Size Optimizing</dd>
        <dt>Minimum lifetime:</dt>
        <dd>{index_lifetime_min} ({minLifetimeAsDays} <Pluralize singular="day" plural="days" value={minLifetimeAsDays} /> )</dd>
        <dt>Maximum lifetime:</dt>
        <dd>{index_lifetime_max} ({maxLifetimeAsDays} <Pluralize singular="day" plural="days" value={maxLifetimeAsDays} />)</dd>
      </dl>
    </div>
  );
};

export default TimeBasedSizeOptimizingStrategySummary;
