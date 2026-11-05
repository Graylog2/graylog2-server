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
import { useTranslation } from 'react-i18next';
import moment from 'moment';

import useMaxIndexRotationLimit from 'hooks/useMaxIndexRotationLimit';
import RangeInput from 'components/common/RangeInput';
import useTimeSizeOptimizingFixedLeeway from 'hooks/useTimeSizeOptimizingFixedLeeway';

export type TimeBasedSizeOptimizingStrategyConfig = {
  index_lifetime_max: string;
  index_lifetime_min: string;
  type: string;
};

type Props = {
  config: TimeBasedSizeOptimizingStrategyConfig;
  updateConfig: (config: Omit<TimeBasedSizeOptimizingStrategyConfig, 'type'>) => void;
  disabled?: boolean;
};

export const durationToRoundedDays = (duration: string) => Math.round(moment.duration(duration).asDays());

const getInitialRangeInDays = (indexLifeTimeMin, indexLifeTimeMax, timeSizeOptimizingFixedLeeway) =>
  timeSizeOptimizingFixedLeeway
    ? [
        durationToRoundedDays(indexLifeTimeMin),
        durationToRoundedDays(indexLifeTimeMin) + durationToRoundedDays(timeSizeOptimizingFixedLeeway),
      ]
    : [durationToRoundedDays(indexLifeTimeMin), durationToRoundedDays(indexLifeTimeMax)];

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
  disabled = false,
}: Props) => {
  const { t } = useTranslation('indexsets');
  const timeSizeOptimizingFixedLeeway = useTimeSizeOptimizingFixedLeeway();
  const [indexLifetimeRange, setIndexLifetimeRange] = useState(
    getInitialRangeInDays(index_lifetime_min, index_lifetime_max, timeSizeOptimizingFixedLeeway),
  );
  const maxRotationPeriod = useMaxIndexRotationLimit();
  const [maxRange, setMaxRange] = useState(
    getMaxRange(
      durationToRoundedDays(maxRotationPeriod),
      indexLifetimeRange[1],
      durationToRoundedDays(timeSizeOptimizingFixedLeeway),
    ),
  );

  const isValidRange = useCallback((range: Array<number>) => range[0] < range[1] && range[1] <= maxRange, [maxRange]);

  const validationState = (range: Array<number>): null | 'error' => {
    if (isValidRange(range)) {
      return null;
    }

    return 'error';
  };

  const errorMessage = t('strategies.rotation_lifetime_error');

  const addYearToMaxRange = (currentMax: number, currentSelectedMax: number) => {
    if (!maxRotationPeriod && currentMax <= currentSelectedMax) {
      setMaxRange(currentMax + YEAR_IN_DAYS);
    }
  };

  const onRangeChange = (range: Array<number> | number) => {
    const currentRange = Array.isArray(range)
      ? range
      : [range, range + durationToRoundedDays(timeSizeOptimizingFixedLeeway)];
    setIndexLifetimeRange(currentRange);
    addYearToMaxRange(maxRange, currentRange[1]);

    if (isValidRange(currentRange)) {
      updateConfig({
        index_lifetime_min: durationToISOString(currentRange[0]),
        index_lifetime_max: durationToISOString(currentRange[1]),
      });
    }
  };

  const rangeHelpText = [
    timeSizeOptimizingFixedLeeway
      ? t('strategies.rotation_lifetime_help_minimum')
      : t('strategies.rotation_lifetime_help_range'),
    maxRotationPeriod &&
      t('strategies.rotation_lifetime_max_rotation', { count: durationToRoundedDays(maxRotationPeriod) }),
    timeSizeOptimizingFixedLeeway &&
      t('strategies.rotation_lifetime_fixed_leeway', {
        max: durationToISOString(indexLifetimeRange[1]),
        leeway: timeSizeOptimizingFixedLeeway,
      }),
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div>
      <RangeInput
        disabled={disabled}
        label={t('strategies.rotation_lifetime_label')}
        id="lifetime-range"
        value={timeSizeOptimizingFixedLeeway ? indexLifetimeRange[0] : indexLifetimeRange}
        help={isValidRange(indexLifetimeRange) ? rangeHelpText : errorMessage}
        min={1}
        step={1}
        bsStyle={validationState(indexLifetimeRange)}
        max={getMaxRange(
          durationToRoundedDays(maxRotationPeriod),
          indexLifetimeRange[1],
          durationToRoundedDays(timeSizeOptimizingFixedLeeway),
        )}
        onAfterChange={(value) => onRangeChange(value)}
      />
    </div>
  );
};

export default TimeBasedSizeOptimizingStrategyConfiguration;
