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
import PropTypes from 'prop-types';
import moment from 'moment';

import useMaxIndexRotationLimit from 'hooks/useMaxIndexRotationLimit';
import RangeInput from 'components/common/RangeInput';

export type TimeBasedSizeOptimizingStrategyConfig = {
  index_lifetime_max: string,
  index_lifetime_min: string,
  type: string,
}

type Props = {
  config: TimeBasedSizeOptimizingStrategyConfig,
  updateConfig: (config: Omit<TimeBasedSizeOptimizingStrategyConfig, 'type'>) => void,
}

export const durationToRoundedDays = (duration: string) => {
  return Math.round(moment.duration(duration).asDays());
};

const getInitialRangeInDays = (indexLifeTimeMin, IndexLifeTimeMax) => {
  return [durationToRoundedDays(indexLifeTimeMin), durationToRoundedDays(IndexLifeTimeMax)];
};

const YEAR_IN_DAYS = 365;

const getInitialMaxRange = (maxRotationPeriod: number, maxLifetime: number) => {
  if (maxRotationPeriod) {
    return maxRotationPeriod;
  }

  return maxLifetime > YEAR_IN_DAYS ? maxLifetime + YEAR_IN_DAYS : YEAR_IN_DAYS;
};

const TimeBasedSizeOptimizingStrategyConfiguration = ({ config: { index_lifetime_max, index_lifetime_min }, updateConfig }: Props) => {
  const [indexLifetimeRange, setIndexLifetimeRange] = useState(getInitialRangeInDays(index_lifetime_min, index_lifetime_max));
  const maxRotationPeriod = useMaxIndexRotationLimit();
  const [maxRange, setMaxRange] = useState(getInitialMaxRange(durationToRoundedDays(maxRotationPeriod), indexLifetimeRange[1]));

  const isValidRange = useCallback((range: Array<number>) => {
    return range[0] < range[1] && range[1] <= maxRange;
  }, [maxRange]);

  const validationState = (range: Array<number>): null | 'error' => {
    if (isValidRange(range)) {
      return null;
    }

    return 'error';
  };

  const errorMessage = 'There needs to be at least 1 day between the minimum and maximum lifetime.';

  const addYearToMaxRange = (currentMax: number, currentSelectedMax: number) => {
    if (!maxRotationPeriod && currentMax === currentSelectedMax) {
      setMaxRange(currentMax + YEAR_IN_DAYS);
    }
  };

  const onRangeChange = (range: Array<number>) => {
    setIndexLifetimeRange(range);
    addYearToMaxRange(maxRange, range[1]);

    if (isValidRange(range)) {
      updateConfig({ index_lifetime_min: moment.duration(range[0], 'days').toISOString(), index_lifetime_max: moment.duration(range[1], 'days').toISOString() });
    }
  };

  const maxRotationPeriodHelpText = maxRotationPeriod ? ` The max rotation period is set to ${durationToRoundedDays(maxRotationPeriod)} days by the Administrator.` : '';

  return (
    <div>
      <RangeInput label="Lifetime in days"
                  id="lifetime-range"
                  labelClassName="col-sm-3"
                  wrapperClassName="col-sm-9"
                  value={indexLifetimeRange}
                  help={isValidRange(indexLifetimeRange) ? `The minimum / maximum number of days the data in this index is kept before it is retained. ${maxRotationPeriodHelpText}` : errorMessage}
                  min={1}
                  step={1}
                  bsStyle={validationState(indexLifetimeRange)}
                  max={durationToRoundedDays(maxRotationPeriod) || maxRange}
                  onAfterChange={(value) => onRangeChange(value)} />
    </div>
  );
};

TimeBasedSizeOptimizingStrategyConfiguration.propTypes = {
  config: PropTypes.object.isRequired,
  updateConfig: PropTypes.func.isRequired,
};

export default TimeBasedSizeOptimizingStrategyConfiguration;
