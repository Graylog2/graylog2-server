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

export const DATE_SEPARATOR = '><';
export const TIME_RANGE_TYPE_SEPARATOR = '@';

export const isRelativeFilterValue = (value: string) => value.startsWith(`relative${TIME_RANGE_TYPE_SEPARATOR}`);
export const isKeywordFilterValue = (value: string) => value.startsWith(`keyword${TIME_RANGE_TYPE_SEPARATOR}`);

export const extractRangeFromString = (timeRange: string) => {
  const inner = timeRange.startsWith(`absolute${TIME_RANGE_TYPE_SEPARATOR}`)
    ? timeRange.slice(`absolute${TIME_RANGE_TYPE_SEPARATOR}`.length)
    : timeRange;

  return inner.split(DATE_SEPARATOR);
};

export const extractRelativeFromString = (value: string): { range?: number; from?: number; to?: number } => {
  const inner = value.slice(`relative${TIME_RANGE_TYPE_SEPARATOR}`.length);

  if (inner.includes(DATE_SEPARATOR)) {
    const [from, to] = inner.split(DATE_SEPARATOR);

    return { from: parseInt(from, 10), to: parseInt(to, 10) };
  }

  return { range: parseInt(inner, 10) };
};

export const extractKeywordFromString = (value: string): string =>
  value.slice(`keyword${TIME_RANGE_TYPE_SEPARATOR}`.length);

// from and until need to be in the user time zone.
export const timeRangeTitle = (from: string, until: string) => `${from || 'All time'} - ${until || 'Now'}`;

export const relativeTimeRangeTitle = (value: string): string => {
  const { range, from, to } = extractRelativeFromString(value);

  if (range !== undefined) {
    return `Last ${moment.duration(range, 'seconds').humanize()}`;
  }

  const fromLabel = moment.duration(from, 'seconds').humanize();

  if (to) {
    return `${fromLabel} - ${moment.duration(to, 'seconds').humanize()} ago`;
  }

  return `${fromLabel} ago - now`;
};

export const keywordTimeRangeTitle = (value: string): string => extractKeywordFromString(value);

export const filterValueTitle = (value: string): string => {
  if (isRelativeFilterValue(value)) return relativeTimeRangeTitle(value);
  if (isKeywordFilterValue(value)) return keywordTimeRangeTitle(value);

  const [from, until] = extractRangeFromString(value);

  return timeRangeTitle(from || undefined, until || undefined);
};
