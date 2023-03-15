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
import isEqual from 'lodash/isEqual';

import type {
  TimeRange,
  NoTimeRangeOverride,
  RelativeTimeRangeStartOnly,
  RelativeTimeRangeWithEnd,
  KeywordTimeRange,
  RelativeTimeRange,
  AbsoluteTimeRange,
} from 'views/logic/queries/Query';

export const isTypeAbsolute = (timeRange: TimeRange | NoTimeRangeOverride): timeRange is AbsoluteTimeRange => {
  return 'type' in timeRange && timeRange.type === 'absolute';
};

export const isTypeRelative = (timeRange: TimeRange | NoTimeRangeOverride): timeRange is RelativeTimeRange => {
  return 'type' in timeRange && timeRange.type === 'relative';
};

export const isTypeRelativeWithStartOnly = (timeRange: TimeRange | NoTimeRangeOverride): timeRange is RelativeTimeRangeStartOnly => {
  return isTypeRelative(timeRange) && 'range' in timeRange;
};

export const isTypeRelativeWithEnd = (timeRange: TimeRange | NoTimeRangeOverride): timeRange is RelativeTimeRangeWithEnd => {
  return isTypeRelative(timeRange) && 'from' in timeRange;
};

export const isTypeKeyword = (timeRange: TimeRange | NoTimeRangeOverride): timeRange is KeywordTimeRange => {
  return 'type' in timeRange && timeRange.type === 'keyword';
};

export const isNoTimeRangeOverride = (timeRange: TimeRange | NoTimeRangeOverride): timeRange is NoTimeRangeOverride => {
  return timeRange !== undefined && isEqual(timeRange, {});
};
