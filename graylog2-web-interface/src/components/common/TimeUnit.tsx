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

import type { TimeUnit as TimeUnitString } from './types';

/**
 * Component that renders a time value given in a certain unit.
 * It can also use 0 as never if `zeroIsNever` is set.
 */
type Props = {
  value: number,
  unit: TimeUnitString,
  zeroIsNever?: boolean,
}

const UNITS = {
  NANOSECONDS: 'nanoseconds',
  MICROSECONDS: 'microseconds',
  MILLISECONDS: 'milliseconds',
  SECONDS: 'seconds',
  MINUTES: 'minutes',
  HOURS: 'hours',
  DAYS: 'days',
};

const TimeUnit = ({ value, unit, zeroIsNever = true }: Props) => {
  if (value === 0 && zeroIsNever) {
    return <span>Never</span>;
  }

  return (
    <span>
      {value}&nbsp;{UNITS[unit]}
    </span>
  );
};

export default TimeUnit;
