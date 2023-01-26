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
import PropTypes from 'prop-types';

import type { TimeBasedSizeOptimizingStrategyConfig } from './TimeBasedSizeOptimizingStrategyConfiguration';
import { durationToRoundedDays } from './TimeBasedSizeOptimizingStrategyConfiguration';

const TimeBasedSizeOptimizingStrategySummary = ({ config: { index_lifetime_hard, index_lifetime_soft } }: {config: TimeBasedSizeOptimizingStrategyConfig}) => {
  return (
    <div>
      <dl>
        <dt>Index rotation strategy:</dt>
        <dd>Index Smart Size/Time</dd>
        <dt>Soft lifetime:</dt>
        <dd>{index_lifetime_soft} ({durationToRoundedDays(index_lifetime_soft)} day(s))</dd>
        <dt>Hard lifetime:</dt>
        <dd>{index_lifetime_hard} ({durationToRoundedDays(index_lifetime_hard)} day(s))</dd>
      </dl>
    </div>
  );
};

TimeBasedSizeOptimizingStrategySummary.propTypes = {
  config: PropTypes.object.isRequired,
};

export default TimeBasedSizeOptimizingStrategySummary;
