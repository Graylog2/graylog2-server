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
import { useTranslation } from 'react-i18next';

import type { TimeUnit as TimeUnitString } from './types';

/**
 * Component that renders a time value given in a certain unit.
 * It can also use 0 as never if `zeroIsNever` is set.
 */
type Props = {
  value: number;
  unit: TimeUnitString;
  zeroIsNever?: boolean;
};

const UNITS = {
  NANOSECONDS: 'duration.nanoseconds',
  MICROSECONDS: 'duration.microseconds',
  MILLISECONDS: 'duration.milliseconds',
  SECONDS: 'duration.seconds',
  MINUTES: 'duration.minutes',
  HOURS: 'duration.hours',
  DAYS: 'duration.days',
} as const;

const TimeUnit = ({ value, unit, zeroIsNever = true }: Props) => {
  const { t } = useTranslation('common');

  if (value === 0 && zeroIsNever) {
    return <span>{t('time.never')}</span>;
  }

  return <span>{t(UNITS[unit], { count: value })}</span>;
};

export default TimeUnit;
