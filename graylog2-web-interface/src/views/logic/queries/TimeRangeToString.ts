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

import 'moment-duration-format';
import 'moment-precise-range-plugin';
import i18n from 'i18n';
import type { AbsoluteTimeRange, KeywordTimeRange, RelativeTimeRange, TimeRange } from 'views/logic/queries/Query';
import { isTypeRelativeWithStartOnly } from 'views/typeGuards/timeRange';
import type { EffectiveTimeRange } from 'components/events/EventsHistogram';

export const readableRange = (timerange: TimeRange, fieldName: 'range' | 'from' | 'to', placeholder?: string) => {
  const rangeAsSeconds = timerange?.[fieldName];
  const allTimeLabel = i18n.t('time_range.all_time', { ns: 'search', defaultValue: 'All Time' });
  const nowLabel = i18n.t('time_range.now', { ns: 'search', defaultValue: 'Now' });
  const defaultFallback = fieldName === 'to' ? nowLabel : allTimeLabel;
  const fallback = placeholder ?? defaultFallback;

  if (!rangeAsSeconds) {
    return fallback;
  }

  const reference = moment();
  const dateAgo = moment(reference).subtract(rangeAsSeconds, 'seconds');
  const rangeTimespan = moment.preciseDiff(reference, dateAgo);

  return i18n.t('time_range.range_ago', { ns: 'search', time: rangeTimespan, defaultValue: '{{time}} ago' });
};

const relativeTimeRangeToString = (timerange: RelativeTimeRange): string => {
  const nowLabel = i18n.t('time_range.now', { ns: 'search', defaultValue: 'Now' });
  const allTimeLabel = i18n.t('time_range.all_time', { ns: 'search', defaultValue: 'All Time' });

  if (isTypeRelativeWithStartOnly(timerange)) {
    if (timerange.range === 0) {
      return allTimeLabel;
    }

    return `${readableRange(timerange, 'range')} - ${nowLabel}`;
  }

  return `${readableRange(timerange, 'from')} - ${readableRange(timerange, 'to')}`;
};

const absoluteTimeRangeToString = (
  timerange: AbsoluteTimeRange,
  localizer = (dateTime: string) => dateTime,
): string => {
  const { from, to } = timerange;

  return `${localizer(from)} - ${localizer(to)}`;
};

const keywordTimeRangeToString = (timerange: KeywordTimeRange): string => timerange.keyword;

const TimeRangeToString = (
  timerange?: TimeRange | EffectiveTimeRange,
  localizer?: (dateTime: string) => string,
): string => {
  const { type } = timerange || {};

  switch (type) {
    case 'relative':
      return relativeTimeRangeToString(timerange as RelativeTimeRange);
    case 'absolute':
      return absoluteTimeRangeToString(timerange as AbsoluteTimeRange, localizer);
    case 'keyword':
      return keywordTimeRangeToString(timerange as KeywordTimeRange);

    default: {
      return '';
    }
  }
};

export default TimeRangeToString;
