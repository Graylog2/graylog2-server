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

import type {
  TimeRange,
  NoTimeRangeOverride,
  KeywordTimeRange,
  RelativeTimeRangeWithEnd,
  AbsoluteTimeRange,
} from 'views/logic/queries/Query';
import DateTime from 'logic/datetimes/DateTime';
import { isTypeAbsolute, isTypeRelativeWithEnd, isTypeKeyword } from 'views/typeGuards/timeRange';

const invalidDateFormatError = 'Format must be: YYYY-MM-DD [HH:mm:ss[.SSS]].';
const rangeLimitError = 'Range is outside limit duration.';
const dateLimitError = 'Date is outside limit duration.';
const timeRangeError = 'The "Until" date must come after the "From" date.';

const exceedsDuration = (timeRange, limitDuration) => {
  if (limitDuration === 0) {
    return false;
  }

  switch (timeRange?.type) {
    case 'absolute':
    case 'keyword': { // eslint-disable-line no-fallthrough, padding-line-between-statements
      const durationFrom = timeRange.from;
      const durationLimit = DateTime.now().subtract(Number(limitDuration), 'seconds').format(DateTime.Formats.TIMESTAMP);

      return moment(durationFrom).isBefore(durationLimit);
    }

    default:
      return false;
  }
};

const validateAbsoluteTimeRange = (timeRange: AbsoluteTimeRange, limitDuration: number) => {
  let errors: {
    from?: string,
    to?: string,
  } = {};

  if (!DateTime.isValidDateString(timeRange.from)) {
    errors = { ...errors, from: invalidDateFormatError };
  }

  if (!DateTime.isValidDateString(timeRange.to)) {
    errors = { ...errors, to: invalidDateFormatError };
  }

  if (timeRange.from >= timeRange.to) {
    errors = { ...errors, to: timeRangeError };
  }

  if (exceedsDuration(timeRange, limitDuration)) {
    errors = { ...errors, from: dateLimitError };
  }

  return errors;
};

const validateRelativeTimeRangeWithEnd = (timeRange: RelativeTimeRangeWithEnd, limitDuration: number) => {
  let errors = {};

  if (limitDuration > 0) {
    if (timeRange.from > limitDuration || !timeRange.from) {
      errors = { ...errors, from: rangeLimitError };
    }

    if (timeRange.to > limitDuration) {
      errors = { ...errors, to: rangeLimitError };
    }
  }

  if (timeRange.from === null) {
    errors = { ...errors, from: 'Cannot be empty.' };
  }

  if (timeRange.from && timeRange.to === null) {
    errors = { ...errors, to: 'Cannot be empty.' };
  }

  if (timeRange.from && timeRange.from <= timeRange.to) {
    errors = { ...errors, to: timeRangeError };
  }

  return errors;
};

const validateKeywordTimeRange = (timeRange: KeywordTimeRange, limitDuration: number) => {
  let errors: { keyword?: string } = {};

  if (exceedsDuration(timeRange, limitDuration)) {
    errors = { keyword: rangeLimitError };
  }

  return errors;
};

const validateTimeRange = (timeRange: TimeRange | NoTimeRangeOverride, limitDuration: number) => {
  let errors = {};

  if (isTypeKeyword(timeRange)) {
    errors = { ...errors, ...validateKeywordTimeRange(timeRange, limitDuration) };
  }

  if (isTypeRelativeWithEnd(timeRange)) {
    errors = { ...errors, ...validateRelativeTimeRangeWithEnd(timeRange, limitDuration) };
  }

  if (isTypeAbsolute(timeRange)) {
    errors = { ...errors, ...validateAbsoluteTimeRange(timeRange, limitDuration) };
  }

  return errors;
};

export default validateTimeRange;
