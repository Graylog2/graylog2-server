import type { TimeRange, RelativeTimeRange } from 'views/logic/queries/Query';
import { isTypeRelativeWithStartOnly, isTypeRelativeWithEnd } from 'views/typeGuards/timeRange';
import assertUnreachable from 'logic/assertUnreachable';

type SearchRelativeRangeStartOnly = {
  rangetype: 'relative',
  relative: string,
};

type SearchRelativeRangeWithEnd = {
  rangetype: 'relative',
  from: string,
  to?: string,
};

type SearchRelativeRange = SearchRelativeRangeStartOnly | SearchRelativeRangeWithEnd

type SearchAbsoluteRange = {
  rangetype: 'absolute',
  from: string,
  to: string,
};

type SearchKeywordRange = {
  rangetype: 'keyword',
  keyword: string,
  timezone?: string,
};

export type SearchTimeRange = SearchAbsoluteRange | SearchRelativeRange | SearchKeywordRange;

export const toSearchTimerange = (timerange: TimeRange): SearchTimeRange => {
  switch (timerange.type) {
    case 'relative':
      if (isTypeRelativeWithStartOnly(timerange)) {
        return { rangetype: 'relative', relative: String(timerange.range) };
      }

      if (isTypeRelativeWithEnd(timerange)) {
        if ('to' in timerange) {
          return { rangetype: 'relative', from: String(timerange.from), to: String(timerange.to) };
        }

        return { rangetype: 'relative', from: String(timerange.from) };
      }

      return assertUnreachable(timerange, 'Unexpected timerange: ');
    case 'keyword': return { rangetype: 'keyword', keyword: timerange.keyword };
    case 'absolute': return { rangetype: 'absolute', from: timerange.from, to: timerange.to };
    default: return assertUnreachable(timerange, 'Unexpected time range type: ');
  }
};

const parseRelativeTimeRange = (range: SearchRelativeRange): RelativeTimeRange => {
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

export const fromSearchTimerange = (range: SearchTimeRange): TimeRange => {
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
