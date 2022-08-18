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

import type { AbsoluteTimeRange, KeywordTimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';
import { DEFAULT_RELATIVE_FROM } from 'views/Constants';
import type { RelativeTimeRangeClassified } from 'views/components/searchbar/date-time-picker/types';
import type { DateTime } from 'util/DateTime';

import {
  classifyFromRange,
  isTypeRelativeClassified,
  normalizeClassifiedRange, RELATIVE_CLASSIFIED_ALL_TIME_RANGE,
} from './RelativeTimeRangeClassifiedHelper';

const getDefaultAbsoluteFromRange = (oldTimeRange: RelativeTimeRangeClassified | AbsoluteTimeRange | KeywordTimeRange | NoTimeRangeOverride | undefined | null) => {
  if (isTypeRelativeClassified(oldTimeRange)) {
    return normalizeClassifiedRange(oldTimeRange.from);
  }

  return DEFAULT_RELATIVE_FROM;
};

const getDefaultAbsoluteToRange = (oldTimeRange: RelativeTimeRangeClassified | AbsoluteTimeRange | KeywordTimeRange | NoTimeRangeOverride | undefined | null) => {
  if (isTypeRelativeClassified(oldTimeRange)) {
    return normalizeClassifiedRange(oldTimeRange.to);
  }

  return 0;
};

const migrationStrategies = {
  absolute: (
    oldTimeRange: RelativeTimeRangeClassified | AbsoluteTimeRange | KeywordTimeRange | NoTimeRangeOverride | undefined | null,
    formatTime: (dateTime: DateTime, tz: string) => string,
  ) => ({
    type: 'absolute',
    from: formatTime(moment().subtract(getDefaultAbsoluteFromRange(oldTimeRange), 'seconds'), 'complete'),
    to: formatTime(moment().subtract(getDefaultAbsoluteToRange(oldTimeRange), 'seconds'), 'complete'),
  }),
  relative: () => ({ type: 'relative', from: classifyFromRange(DEFAULT_RELATIVE_FROM), to: RELATIVE_CLASSIFIED_ALL_TIME_RANGE }),
  keyword: () => ({ type: 'keyword', keyword: 'Last five minutes' }),
  disabled: () => undefined,
};

const migrateTimeRangeToNewType = (
  oldTimeRange: RelativeTimeRangeClassified | AbsoluteTimeRange | KeywordTimeRange | NoTimeRangeOverride | undefined | null,
  type: string,
  formatTime: (dateTime: DateTime, tz: string) => string,
): RelativeTimeRangeClassified | AbsoluteTimeRange | KeywordTimeRange | NoTimeRangeOverride | undefined | null => {
  const oldType = oldTimeRange && 'type' in oldTimeRange ? oldTimeRange.type : 'disabled';

  if (type === oldType) {
    return oldTimeRange;
  }

  if (!migrationStrategies[type]) {
    throw new Error(`Invalid time range type: ${type}`);
  }

  return migrationStrategies[type](oldTimeRange, formatTime);
};

export default migrateTimeRangeToNewType;
