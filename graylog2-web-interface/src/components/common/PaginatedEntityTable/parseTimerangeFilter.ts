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
import trim from 'lodash/trim';

import {
  DATE_SEPARATOR,
  TIME_RANGE_TYPE_SEPARATOR,
  extractKeywordFromString,
  extractRangeFromString,
  extractRelativeFromString,
  isKeywordFilterValue,
  isRelativeFilterValue,
} from 'components/common/EntityFilters/helpers/timeRange';
import { adjustFormat } from 'util/DateTime';
import type { TimeRange } from 'views/logic/queries/Query';

const isNullOrBlank = (s: string | undefined) => {
  if (!s) {
    return true;
  }

  return trim(s) === '';
};

const parseTimerangeFilter = (timestamp: string | undefined, defaultTimerange?: TimeRange): TimeRange => {
  if (!timestamp) {
    return defaultTimerange;
  }

  if (isRelativeFilterValue(timestamp)) {
    const { range, from, to } = extractRelativeFromString(timestamp);

    if (range !== undefined) {
      return { type: 'relative', range };
    }

    return { type: 'relative', from, to };
  }

  if (isKeywordFilterValue(timestamp)) {
    return { type: 'keyword', keyword: extractKeywordFromString(timestamp) };
  }

  const [from, to] = extractRangeFromString(timestamp);

  if (!from && !to) {
    return defaultTimerange;
  }

  return {
    type: 'absolute',
    from: isNullOrBlank(from) ? adjustFormat(moment(0).utc(), 'internal') : from,
    to: isNullOrBlank(to) ? adjustFormat(moment().utc(), 'internal') : to,
  };
};

export const timeRangeToFilterValue = (timeRange: TimeRange): string => {
  switch (timeRange.type) {
    case 'absolute':
      return `${timeRange.from}${DATE_SEPARATOR}${timeRange.to}`;
    case 'relative':
      if ('range' in timeRange) {
        return `relative${TIME_RANGE_TYPE_SEPARATOR}${timeRange.range}`;
      }

      if (timeRange.to === undefined) {
        return `relative${TIME_RANGE_TYPE_SEPARATOR}${timeRange.from}`;
      }

      return `relative${TIME_RANGE_TYPE_SEPARATOR}${timeRange.from}${DATE_SEPARATOR}${timeRange.to}`;
    case 'keyword':
      return `keyword${TIME_RANGE_TYPE_SEPARATOR}${timeRange.keyword}`;
    default:
      return '';
  }
};

export default parseTimerangeFilter;
