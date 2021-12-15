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
import moment from 'moment';

import { RELATIVE_RANGE_TYPES, RELATIVE_ALL_TIME } from 'views/Constants';
import type {
  RelativeTimeRange,
  AbsoluteTimeRange,
  KeywordTimeRange,
  NoTimeRangeOverride, TimeRange,
} from 'views/logic/queries/Query';
import type { RelativeTimeRangeClassified, RangeClassified } from 'views/components/searchbar/date-time-picker/types';
import { isTypeRelativeWithStartOnly } from 'views/typeGuards/timeRange';

const DEFAULT_CLASSIFIED_RANGE: RangeClassified = {
  value: 5,
  unit: 'minutes',
  isAllTime: false,
};

export const RELATIVE_CLASSIFIED_ALL_TIME_RANGE: RangeClassified = {
  value: 0,
  unit: 'seconds',
  isAllTime: true,
};

export const classifyRange = (range: number | undefined | null, allTimeValue: number | undefined) => {
  if (range === null) {
    return DEFAULT_CLASSIFIED_RANGE;
  }

  if (range === allTimeValue) {
    return RELATIVE_CLASSIFIED_ALL_TIME_RANGE;
  }

  return RELATIVE_RANGE_TYPES.map(({ type }) => {
    const diff = moment.duration(range, 'seconds').as(type);

    if (diff - Math.floor(diff) === 0) {
      return {
        value: diff || 0,
        unit: type,
        isAllTime: false,
      };
    }

    return null;
  }).filter(Boolean).pop();
};

export const classifyFromRange = (range: number) => classifyRange(range, RELATIVE_ALL_TIME);
export const classifyToRange = (range: number) => classifyRange(range, undefined);

export const classifyRelativeTimeRange = (timeRange: RelativeTimeRange): RelativeTimeRangeClassified => {
  if (isTypeRelativeWithStartOnly(timeRange)) {
    return {
      type: 'relative',
      from: classifyFromRange(timeRange.range),
      to: classifyToRange(undefined),
    };
  }

  return {
    type: 'relative',
    from: classifyFromRange(timeRange.from),
    to: classifyToRange(timeRange.to),
  };
};

export const isTypeRelativeClassified = (timeRange): timeRange is RelativeTimeRangeClassified => {
  return (timeRange.type === 'relative' && typeof timeRange.from === 'object' && typeof timeRange.to === 'object');
};

export const normalizeClassifiedRange = ({ value, unit, isAllTime }: RangeClassified) => {
  if (isAllTime) {
    return RELATIVE_ALL_TIME;
  }

  return moment.duration(value || 1, unit).asSeconds();
};

export const normalizeIfClassifiedRelativeTimeRange = (timeRange: RelativeTimeRangeClassified | AbsoluteTimeRange | KeywordTimeRange | NoTimeRangeOverride): TimeRange | NoTimeRangeOverride => {
  if (isTypeRelativeClassified(timeRange)) {
    const fromRange = timeRange.from.value !== null ? normalizeClassifiedRange(timeRange.from) : null;
    const toRange = timeRange.to.value !== null ? normalizeClassifiedRange(timeRange.to) : null;

    if (timeRange.to.isAllTime) {
      return {
        type: 'relative',
        from: fromRange,
      };
    }

    return {
      type: 'relative',
      from: fromRange,
      to: toRange,
    };
  }

  return timeRange;
};
