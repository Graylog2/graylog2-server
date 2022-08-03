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
import type { TimeRange, RelativeTimeRange } from 'views/logic/queries/Query';
import { isTypeRelativeWithStartOnly, isTypeRelativeWithEnd } from 'views/typeGuards/timeRange';
import assertUnreachable from 'logic/assertUnreachable';

type RelativeRangeStartOnlyQueryParameter = {
  rangetype: 'relative',
  relative: string,
};

type RelativeRangeWithEndQueryParameter = {
  rangetype: 'relative',
  from: string,
  to?: string,
};

type RelativeRangeQueryParameter = RelativeRangeStartOnlyQueryParameter | RelativeRangeWithEndQueryParameter;

type AbsoluteRangeQueryParameter = {
  rangetype: 'absolute',
  from: string,
  to: string,
};

type KeywordRangeQueryParameter = {
  rangetype: 'keyword',
  keyword: string,
  timezone?: string,
};

export type TimeRangeQueryParameter = AbsoluteRangeQueryParameter | RelativeRangeQueryParameter | KeywordRangeQueryParameter;

export const timeRangeToQueryParameter = (timeRange: TimeRange): TimeRangeQueryParameter => {
  switch (timeRange.type) {
    case 'relative':
      if (isTypeRelativeWithStartOnly(timeRange)) {
        return { rangetype: 'relative', relative: String(timeRange.range) };
      }

      if (isTypeRelativeWithEnd(timeRange)) {
        if ('to' in timeRange) {
          return { rangetype: 'relative', from: String(timeRange.from), to: String(timeRange.to) };
        }

        return { rangetype: 'relative', from: String(timeRange.from) };
      }

      return assertUnreachable(timeRange, 'Unexpected timeRange: ');
    case 'keyword': return { rangetype: 'keyword', keyword: timeRange.keyword };
    case 'absolute': return { rangetype: 'absolute', from: timeRange.from, to: timeRange.to };
    default: return assertUnreachable(timeRange, 'Unexpected time range type: ');
  }
};

const parseRelativeTimeRange = (range: RelativeRangeQueryParameter): RelativeTimeRange => {
  const parseRangeValue = (rangeValue: string) => parseInt(rangeValue, 10);

  if ('relative' in range) {
    return { type: 'relative', range: parseRangeValue(range.relative) };
  }

  if ('from' in range) {
    const result = { type: 'relative' as const, from: parseRangeValue(range.from) };

    if ('to' in range) {
      return { ...result, to: parseRangeValue(range.to) };
    }

    return result;
  }

  return assertUnreachable(range, 'Invalid relative range specified: ');
};

export const timeRangeFromQueryParameter = (range: TimeRangeQueryParameter): TimeRange => {
  switch (range?.rangetype) {
    case 'relative':
      return parseRelativeTimeRange(range);
    case 'absolute':
      return ('from' in range && 'to' in range) ? {
        type: range.rangetype,
        from: range.from,
        to: range.to,
      } : assertUnreachable(range, 'Invalid absolute range specified: ');
    case 'keyword':
      return 'keyword' in range ? {
        type: range.rangetype,
        keyword: range.keyword,
        timezone: range.timezone,
      } : assertUnreachable(range, 'Invalid keyword range specified: ');
    default:
      return assertUnreachable(range, 'Unsupported range type in range: ');
  }
};
