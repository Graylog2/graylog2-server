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
import { useState, useCallback } from 'react';
import moment from 'moment';

import useMaxIndexRotationLimit from 'hooks/useMaxIndexRotationLimit';
import RangeInput from 'components/common/RangeInput';
import useTimeSizeOptimizingFixedLeeway from 'hooks/useTimeSizeOptimizingFixedLeeway';

export type TimeBasedSizeOptimizingStrategyConfig = {
  index_lifetime_max: string,
  index_lifetime_min: string,
  type: string,
}

type Props = {
  config: TimeBasedSizeOptimizingStrategyConfig,
  updateConfig: (config: Omit<TimeBasedSizeOptimizingStrategyConfig, 'type'>) => void,
}

export const durationToRoundedDays = (duration: string) => Math.round(moment.duration(duration).asDays());

const getInitialRangeInDays = (indexLifeTimeMin, indexLifeTimeMax, timeSizeOptimizingFixedLeeway) => (
  timeSizeOptimizingFixedLeeway
    ? [durationToRoundedDays(indexLifeTimeMin), durationToRoundedDays(indexLifeTimeMin) + durationToRoundedDays(timeSizeOptimizingFixedLeeway)]
    : [durationToRoundedDays(indexLifeTimeMin), durationToRoundedDays(indexLifeTimeMax)]
);

const YEAR_IN_DAYS = 365;

const getMaxRange = (maxRotationPeriod: number, maxLifetime: number, timeSizeOptimizingFixedLeeway: number | null) => {
  if (maxRotationPeriod) {
    return timeSizeOptimizingFixedLeeway ? maxRotationPeriod - timeSizeOptimizingFixedLeeway : maxRotationPeriod;
  }

  return maxLifetime >= YEAR_IN_DAYS ? maxLifetime + YEAR_IN_DAYS : YEAR_IN_DAYS;
};

const durationToISOString = (days: number) => moment.duration(days, 'days').toISOString();

const TimeBasedSizeOptimizingStrategyConfiguration = ({
  config: { index_lifetime_max, index_lifetime_min },
  updateConfig,
}: Props) => {
  const timeSizeOptimizingFixedLeeway = useTimeSizeOptimizingFixedLeeway();
  const [indexLifetimeRange, setIndexLifetimeRange] = useState(getInitialRangeInDays(index_lifetime_min, index_lifetime_max, timeSizeOptimizingFixedLeeway));
  const maxRotationPeriod = useMaxIndexRotationLimit();
  const [maxRange, setMaxRange] = useState(getMaxRange(durationToRoundedDays(maxRotationPeriod), indexLifetimeRange[1], durationToRoundedDays(timeSizeOptimizingFixedLeeway)));

  const isValidRange = useCallback((range: Array<number>) => range[0] < range[1] && range[1] <= maxRange, [maxRange]);

  const validationState = (range: Array<number>): null | 'error' => {
    if (isValidRange(range)) {
      return null;
    }

    return 'error';
  };

  const errorMessage = 'There needs to be at least 1 day between the minimum and maximum lifetime.';

  const addYearToMaxRange = (currentMax: number, currentSelectedMax: number) => {
    if (!maxRotationPeriod && currentMax <= currentSelectedMax) {
      setMaxRange(currentMax + YEAR_IN_DAYS);
    }
  };

  const onRangeChange = (range: Array<number> | number) => {
    const currentRange = Array.isArray(range) ? range : [range, range + durationToRoundedDays(timeSizeOptimizingFixedLeeway)];
    setIndexLifetimeRange(currentRange);
    addYearToMaxRange(maxRange, currentRange[1]);

    if (isValidRange(currentRange)) {
      updateConfig({
        index_lifetime_min: durationToISOString(currentRange[0]),
        index_lifetime_max: durationToISOString(currentRange[1]),
      });
    }
  };

  const maxRotationPeriodHelpText = maxRotationPeriod ? ` The max rotation period is set to ${durationToRoundedDays(maxRotationPeriod)} days by the Administrator.` : '';
  const rangeHelpTitle = timeSizeOptimizingFixedLeeway ? 'minimum' : 'minimum / maximum';
  const fixedLeewayHint = timeSizeOptimizingFixedLeeway ? ` The maximum number of days is ${durationToISOString(indexLifetimeRange[1])} because the fixed number of days between min and max is set to ${timeSizeOptimizingFixedLeeway}.` : '';

  return (
    <div>
      <RangeInput label="Lifetime in days"
                  id="lifetime-range"
                  value={timeSizeOptimizingFixedLeeway ? indexLifetimeRange[0] : indexLifetimeRange}
                  help={isValidRange(indexLifetimeRange) ? `The ${rangeHelpTitle} number of days the data in this index is kept before it is retained. ${maxRotationPeriodHelpText} ${fixedLeewayHint}` : errorMessage}
                  min={1}
                  step={1}
                  bsStyle={validationState(indexLifetimeRange)}
                  max={getMaxRange(durationToRoundedDays(maxRotationPeriod), indexLifetimeRange[1], durationToRoundedDays(timeSizeOptimizingFixedLeeway))}
                  onAfterChange={(value) => onRangeChange(value)} />
    </div>
  );
};

export default TimeBasedSizeOptimizingStrategyConfiguration;
