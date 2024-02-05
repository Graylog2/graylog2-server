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

import isAllMessagesRange from 'views/logic/queries/IsAllMessagesRange';
import { NO_TIMERANGE_OVERRIDE, RELATIVE_ALL_TIME } from 'views/Constants';
import {
  normalizeIfClassifiedRelativeTimeRange,
} from 'views/components/searchbar/time-range-filter/time-range-picker/RelativeTimeRangeClassifiedHelper';
import { isTypeKeyword, isTypeRelativeWithEnd, isTypeRelativeWithStartOnly } from 'views/typeGuards/timeRange';
import { adjustFormat, toUTCFromTz } from 'util/DateTime';
import type { TimeRangePickerTimeRange } from 'views/components/searchbar/time-range-filter/time-range-picker/TimeRangePicker';

import type { TimeRange, NoTimeRangeOverride } from './Query';

export const normalizeIfAllMessagesRange = (timeRange: TimeRange | NoTimeRangeOverride | undefined) => {
  if ('type' in timeRange && isAllMessagesRange(timeRange)) {
    return {
      type: timeRange.type,
      range: RELATIVE_ALL_TIME,
    };
  }

  return timeRange;
};

export const normalizeFromPickerForSearchBar = (timeRange: TimeRangePickerTimeRange | undefined) => {
  if (!timeRange) {
    return NO_TIMERANGE_OVERRIDE;
  }

  const normalizeIfKeywordTimeRange = (tr: TimeRange | NoTimeRangeOverride) => {
    if (isTypeKeyword(tr)) {
      return {
        type: tr.type,
        timezone: tr.timezone,
        keyword: tr.keyword,
      };
    }

    return tr;
  };

  return normalizeIfKeywordTimeRange(
    normalizeIfAllMessagesRange(
      normalizeIfClassifiedRelativeTimeRange(timeRange),
    ),
  );
};

export const normalizeFromSearchBarForBackend = (timerange: TimeRange, userTz: string): TimeRange => {
  const { type } = timerange;

  switch (timerange.type) {
    case 'absolute':
      return {
        type: timerange.type,
        from: adjustFormat(toUTCFromTz(timerange.from, userTz), 'internal'),
        to: adjustFormat(toUTCFromTz(timerange.to, userTz), 'internal'),
      };
    case 'relative':
      if (isTypeRelativeWithStartOnly(timerange)) {
        return {
          type: timerange.type,
          range: timerange.range,
        };
      }

      if (isTypeRelativeWithEnd(timerange)) {
        if ('to' in timerange) {
          return {
            type: timerange.type,
            from: timerange.from,
            to: timerange.to,
          };
        }

        return {
          type: timerange.type,
          from: timerange.from,
        };
      }

      throw new Error('Invalid relative time range');
    case 'keyword':
      return timerange;
    default: throw new Error(`Invalid time range type: ${type}`);
  }
};
