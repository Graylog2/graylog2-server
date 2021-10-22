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

import 'moment-duration-format';
import 'moment-precise-range-plugin';
import type { Moment } from 'moment';

import { AbsoluteTimeRange, KeywordTimeRange, RelativeTimeRange, TimeRange } from 'views/logic/queries/Query';
import { isTypeRelativeWithStartOnly } from 'views/typeGuards/timeRange';

export const readableRange = (timerange: TimeRange, fieldName: 'range' | 'from' | 'to', unifyTimeAsDate: (time: Date) => Moment, placeholder: string | undefined = 'All Time') => {
  return !timerange[fieldName] ? placeholder : unifyTimeAsDate(new Date())
    .subtract(timerange[fieldName] * 1000)
    .fromNow();
};

const relativeTimeRangeToString = (timerange: RelativeTimeRange, unifyAsDate: (time: Date) => Moment): string => {
  if (isTypeRelativeWithStartOnly(timerange)) {
    if (timerange.range === 0) {
      return 'All Time';
    }

    return `${readableRange(timerange, 'range', unifyAsDate)} - Now`;
  }

  return `${readableRange(timerange, 'from', unifyAsDate)} - ${readableRange(timerange, 'to', unifyAsDate, 'Now')}`;
};

const absoluteTimeRangeToString = (timerange: AbsoluteTimeRange, localizer = (str) => str): string => {
  const { from, to } = timerange;

  return `${localizer(from)} - ${localizer(to)}`;
};

const keywordTimeRangeToString = (timerange: KeywordTimeRange): string => {
  return timerange.keyword;
};

const TimeRangeToString = (timerange: TimeRange, unifyAsDate: (time: Date) => Moment, localizer?: (string) => string): string => {
  const { type } = timerange || {};

  switch (type) {
    case 'relative': return relativeTimeRangeToString(timerange as RelativeTimeRange, unifyAsDate);
    case 'absolute': return absoluteTimeRangeToString(timerange as AbsoluteTimeRange, localizer);
    case 'keyword': return keywordTimeRangeToString(timerange as KeywordTimeRange);

    default: {
      return '';
    }
  }
};

export default TimeRangeToString;
