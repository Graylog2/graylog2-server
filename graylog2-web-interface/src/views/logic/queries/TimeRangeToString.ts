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

import { AbsoluteTimeRange, KeywordTimeRange, RelativeTimeRange, TimeRange } from 'views/logic/queries/Query';
import { isTypeRelativeWithStartOnly } from 'views/typeGuards/timeRange';
import DateTime from 'logic/datetimes/DateTime';

export const readableRange = (timerange: TimeRange, fieldName: 'range' | 'from' | 'to', placeholder = 'All Time') => {
  return !timerange[fieldName] ? placeholder : DateTime.now()
    .subtract(timerange[fieldName] * 1000)
    .fromNow();
};

const relativeTimeRangeToString = (timerange: RelativeTimeRange): string => {
  if (isTypeRelativeWithStartOnly(timerange)) {
    if (timerange.range === 0) {
      return 'All Time';
    }

    return `${readableRange(timerange, 'range')} - Now`;
  }

  return `${readableRange(timerange, 'from')} - ${readableRange(timerange, 'to', 'Now')}`;
};

const absoluteTimeRangeToString = (timerange: AbsoluteTimeRange, localizer = (str) => str): string => {
  const { from, to } = timerange;

  return `${localizer(from)} - ${localizer(to)}`;
};

const keywordTimeRangeToString = (timerange: KeywordTimeRange): string => {
  return timerange.keyword;
};

const TimeRangeToString = (timerange?: TimeRange, localizer?: (string) => string): string => {
  const { type } = timerange || {};

  switch (type) {
    case 'relative': return relativeTimeRangeToString(timerange as RelativeTimeRange);
    case 'absolute': return absoluteTimeRangeToString(timerange as AbsoluteTimeRange, localizer);
    case 'keyword': return keywordTimeRangeToString(timerange as KeywordTimeRange);

    default: {
      return '';
    }
  }
};

export default TimeRangeToString;
