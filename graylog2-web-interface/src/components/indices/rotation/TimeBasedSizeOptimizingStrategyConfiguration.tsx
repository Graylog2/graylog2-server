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

const getRangeInDays = (indexLifeTimeMin = 'PT1H', IndexLifeTimeMax = 'P4D') => {
  return [durationToRoundedDays(indexLifeTimeMin), durationToRoundedDays(IndexLifeTimeMax)];
};

const TimeBasedSizeOptimizingStrategyConfiguration = ({ config: { index_lifetime_max, index_lifetime_min }, updateConfig }: Props) => {
  const [indexLifetimeRange, setIndexLifetimeRange] = useState(getRangeInDays(index_lifetime_min, index_lifetime_max));

  const maxRotationPeriod = useMaxIndexRotationLimit();

  const _isValidRange = useCallback((range: Array<number>) => {
    return range[0] !== range[1];
  }, []);

  const validationState = (range: Array<number>): null | 'error' => {
    if (_isValidRange(range)) {
      return null;
    }

    return 'error';
  };

  const errorMessage = 'There should be at least 1 days beteween minimum and maximum Lifetime.';

  const onRangeChange = (range: Array<number>) => {
    setIndexLifetimeRange(range);

    if (_isValidRange(range)) {
      updateConfig({ index_lifetime_min: moment.duration(range[0], 'days').toISOString(), index_lifetime_max: moment.duration(range[1], 'days').toISOString() });
    }
  };

  const maxRotationPeriodHelpText = maxRotationPeriod ? ` The max rotation period is set to ${durationToRoundedDays(maxRotationPeriod)} by Administrator.` : '';

  return (
    <div>
      <RangeInput label="Lifetime Range"
                  id="lifetime-range"
                  labelClassName="col-sm-3"
                  wrapperClassName="col-sm-9"
                  value={indexLifetimeRange}
                  help={_isValidRange(indexLifetimeRange) ? `The range of minimum and maximum time data is index kept before it is rotated. (i.e. "P1D" for 1 day).${maxRotationPeriodHelpText}` : errorMessage}
                  min={1}
                  step={1}
                  bsStyle={validationState(indexLifetimeRange)}
                  max={durationToRoundedDays(maxRotationPeriod) || 365}
                  onAfterChange={(value) => onRangeChange(value)} />
    </div>
  );
};

TimeBasedSizeOptimizingStrategyConfiguration.propTypes = {
  config: PropTypes.object.isRequired,
  updateConfig: PropTypes.func.isRequired,
};

export default TimeBasedSizeOptimizingStrategyConfiguration;
