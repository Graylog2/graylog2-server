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
import type { DateTime, DateTimeFormats } from 'util/DateTime';
import type { TimeRangePickerFormValues } from 'views/components/time-range-picker/types';
import {
  classifyFromRange,
  classifyToRange,
  RELATIVE_CLASSIFIED_ALL_TIME_RANGE,
} from 'views/components/time-range-picker/RelativeTimeRangeClassifiedHelper';
import {
  normalizeFromPickerForSearchBar,
  normalizeFromSearchBarForBackend,
} from 'views/logic/queries/NormalizeTimeRange';
import type { AbsoluteTimeRange, KeywordTimeRange, RelativeTimeRange, TimeRange } from 'views/logic/queries/Query';
import { readableRange } from 'views/logic/queries/TimeRangeToString';
import { isTypeRelativeWithStartOnly } from 'views/typeGuards/timeRange';

export const DATE_SEPARATOR = '><';
export const TIME_RANGE_TYPE_SEPARATOR = '@';

type FormatTime = (dateTime: DateTime, format?: DateTimeFormats) => string;

export const extractRangeFromString = (timeRange: string) => timeRange.split(DATE_SEPARATOR);

const decodeKeywordValue = (value: string) => decodeURIComponent(value.replace(/\+/g, '%20'));
const decodeAbsoluteValue = (value: string) => decodeURIComponent(value);

const isNumericRangeValue = (value: string | undefined) =>
  value !== undefined && value !== '' && Number.isFinite(Number(value));

const parseRelativeTimeRange = (value: string): RelativeTimeRange => {
  const [from, to] = value.split(DATE_SEPARATOR);
  const fromRange = Number(from);

  if (!Number.isFinite(fromRange)) {
    throw new Error(`Invalid relative time range value: ${value}`);
  }

  if (to !== undefined && to !== '') {
    const toRange = Number(to);

    return {
      type: 'relative',
      from: fromRange,
      ...(Number.isFinite(toRange) && { to: toRange }),
    };
  }

  return {
    type: 'relative',
    range: fromRange,
  };
};

const parseAbsoluteTimeRange = (value: string): AbsoluteTimeRange => {
  const [from, to = ''] = value.split(DATE_SEPARATOR);

  return {
    type: 'absolute',
    from: decodeAbsoluteValue(from),
    to: decodeAbsoluteValue(to),
  };
};

const parseUntypedTimeRange = (value: string): TimeRange => {
  const ranges = value.split(DATE_SEPARATOR);
  const isRelative = ranges.every((range) => range === '' || isNumericRangeValue(range));

  return isRelative ? parseRelativeTimeRange(value) : parseAbsoluteTimeRange(value);
};

export const parseStringTimerangeFilterValue = (filterValue: string): TimeRange => {
  const separatorIndex = filterValue.indexOf(TIME_RANGE_TYPE_SEPARATOR);

  if (separatorIndex < 0) {
    return parseUntypedTimeRange(filterValue);
  }

  const type = filterValue.slice(0, separatorIndex);
  const value = filterValue.slice(separatorIndex + TIME_RANGE_TYPE_SEPARATOR.length);

  switch (type) {
    case 'relative':
      return parseRelativeTimeRange(value);
    case 'keyword':
      return {
        type,
        keyword: decodeKeywordValue(value),
      } satisfies KeywordTimeRange;
    case 'absolute':
      return parseAbsoluteTimeRange(value);
    default:
      return parseUntypedTimeRange(filterValue);
  }
};

const relativeTimeRangeTitle = (timeRange: RelativeTimeRange) => {
  if (isTypeRelativeWithStartOnly(timeRange) && timeRange.range === 0) {
    return 'All time';
  }

  const from = isTypeRelativeWithStartOnly(timeRange)
    ? readableRange(timeRange, 'range')
    : readableRange(timeRange, 'from');
  const until = 'to' in timeRange ? readableRange(timeRange, 'to', 'now') : 'now';

  return `from ${from} until ${until}`;
};

const absoluteTimeRangeTitle = (timeRange: AbsoluteTimeRange, formatTime: FormatTime) => {
  const from = timeRange.from ? formatTime(timeRange.from) : 'All time';
  const until = timeRange.to ? formatTime(timeRange.to) : 'Now';

  return `${from} - ${until}`;
};

const defaultInitialValues = (): TimeRangePickerFormValues => ({
  timeRangeTabs: {
    relative: {
      type: 'relative',
      from: {
        value: 5,
        unit: 'minutes',
        isAllTime: false,
      },
      to: RELATIVE_CLASSIFIED_ALL_TIME_RANGE,
    },
  },
  activeTab: 'relative',
});

const relativeTimeRangeToFormValue = (timeRange: RelativeTimeRange) => {
  if (isTypeRelativeWithStartOnly(timeRange)) {
    return {
      type: 'relative' as const,
      from: classifyFromRange(timeRange.range),
      to: RELATIVE_CLASSIFIED_ALL_TIME_RANGE,
    };
  }

  return {
    type: 'relative' as const,
    from: classifyFromRange(timeRange.from),
    to: typeof timeRange.to === 'number' ? classifyToRange(timeRange.to) : RELATIVE_CLASSIFIED_ALL_TIME_RANGE,
  };
};

const absoluteTimeRangeToFormValue = (timeRange: AbsoluteTimeRange, formatTime: FormatTime) => ({
  type: 'absolute' as const,
  from: timeRange.from ? formatTime(timeRange.from, 'complete') : '',
  to: timeRange.to ? formatTime(timeRange.to, 'complete') : formatTime(new Date(), 'complete'),
});

const timeRangeToFormValues = (timeRange: TimeRange, formatTime: FormatTime): TimeRangePickerFormValues => {
  switch (timeRange.type) {
    case 'relative':
      return {
        timeRangeTabs: {
          relative: relativeTimeRangeToFormValue(timeRange),
        },
        activeTab: 'relative',
      };
    case 'keyword':
      return {
        timeRangeTabs: {
          keyword: timeRange,
        },
        activeTab: 'keyword',
      };
    case 'absolute':
      return {
        timeRangeTabs: {
          absolute: absoluteTimeRangeToFormValue(timeRange, formatTime),
        },
        activeTab: 'absolute',
      };
    default:
      throw new Error(`Invalid time range type: ${timeRange}`);
  }
};

const serializeTimeRange = (timeRange: TimeRange | undefined) => {
  if (!timeRange) {
    return '';
  }

  switch (timeRange.type) {
    case 'relative':
      if ('range' in timeRange) {
        return `relative${TIME_RANGE_TYPE_SEPARATOR}${timeRange.range}`;
      }

      if (typeof timeRange.to === 'number') {
        return `relative${TIME_RANGE_TYPE_SEPARATOR}${timeRange.from}${DATE_SEPARATOR}${timeRange.to}`;
      }

      return `relative${TIME_RANGE_TYPE_SEPARATOR}${timeRange.from}`;
    case 'keyword':
      return `keyword${TIME_RANGE_TYPE_SEPARATOR}${timeRange.keyword}`;
    case 'absolute':
      return `absolute${TIME_RANGE_TYPE_SEPARATOR}${timeRange.from}${DATE_SEPARATOR}${timeRange.to}`;
    default:
      throw new Error(`Invalid time range type: ${timeRange}`);
  }
};

export const timeRangeTitle = (timeRange: TimeRange | string | undefined, formatTime: FormatTime) => {
  if (!timeRange) {
    return '';
  }

  const parsedTimeRange = typeof timeRange === 'string' ? parseStringTimerangeFilterValue(timeRange) : timeRange;

  switch (parsedTimeRange.type) {
    case 'relative':
      return relativeTimeRangeTitle(parsedTimeRange);
    case 'keyword':
      return parsedTimeRange.keyword;
    case 'absolute':
      return absoluteTimeRangeTitle(parsedTimeRange, formatTime);
    default:
      throw new Error(`Invalid time range type: ${parsedTimeRange}`);
  }
};

export const filterValueToTimeRangePickerFormValues = (filterValue: string | undefined, formatTime: FormatTime) => {
  if (!filterValue) {
    return defaultInitialValues();
  }

  return timeRangeToFormValues(parseStringTimerangeFilterValue(filterValue), formatTime);
};

export const timeRangePickerFormValuesToFilterValue = (
  { timeRangeTabs, activeTab }: TimeRangePickerFormValues,
  userTimezone: string,
  formatTime: FormatTime,
) => {
  const activeTimeRange = activeTab ? timeRangeTabs[activeTab] : undefined;
  const searchBarTimeRange = normalizeFromPickerForSearchBar(activeTimeRange);
  const backendTimeRange = normalizeFromSearchBarForBackend(searchBarTimeRange, userTimezone);

  return {
    title: timeRangeTitle(backendTimeRange, formatTime),
    value: serializeTimeRange(backendTimeRange),
  };
};
