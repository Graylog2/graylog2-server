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

import type {
  TimeRange,
  NoTimeRangeOverride,
  KeywordTimeRange,
  RelativeTimeRangeWithEnd,
  AbsoluteTimeRange,
} from 'views/logic/queries/Query';
import { isTypeAbsolute, isTypeRelativeWithEnd, isTypeKeyword } from 'views/typeGuards/timeRange';
import type { DateTime } from 'util/DateTime';
import { isValidDate } from 'util/DateTime';
import debounceWithPromise from 'views/logic/debounceWithPromise';
import { testNaturalDate } from 'api/tools';

const invalidDateFormatError = 'Format must be: YYYY-MM-DD [HH:mm:ss[.SSS]].';
const rangeLimitError = 'Range is outside limit duration.';
const timeRangeError = 'The "Until" date must come after the "From" date.';

const exceedsDuration = (from: DateTime, to: DateTime, limitDuration: number) =>
  limitDuration > 0 && moment(to).diff(moment(from), 'seconds') > limitDuration;

const validateAbsoluteTimeRange = (timeRange: AbsoluteTimeRange, limitDuration: number) => {
  let errors: {
    from?: string;
    to?: string;
  } = {};

  if (!isValidDate(timeRange.from)) {
    errors = { ...errors, from: invalidDateFormatError };
  }

  if (!isValidDate(timeRange.to)) {
    errors = { ...errors, to: invalidDateFormatError };
  }

  if (timeRange.from >= timeRange.to) {
    errors = { ...errors, to: timeRangeError };
  }

  if (exceedsDuration(timeRange.from, timeRange.to, limitDuration)) {
    errors = { ...errors, from: rangeLimitError };
  }

  return errors;
};

const validateRelativeTimeRangeWithEnd = (timeRange: RelativeTimeRangeWithEnd, limitDuration: number) => {
  let errors: { from?: string; to?: string } = {};

  if (limitDuration > 0) {
    if (!timeRange.from || timeRange.from - (timeRange.to ?? 0) > limitDuration) {
      errors = { ...errors, from: rangeLimitError };
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

const debouncedTestNaturalDate = debounceWithPromise(testNaturalDate, 350);

const validateKeywordTimeRange = async (
  timeRange: KeywordTimeRange,
  limitDuration: number,
  userTimezone: string,
  testKeyword: boolean,
) => {
  let errors: { keyword?: string } = {};

  if (trim(timeRange.keyword) === '') {
    errors = { keyword: 'Keyword must not be empty!' };

    return errors;
  }

  if (testKeyword) {
    const actualTimeRange = await debouncedTestNaturalDate(timeRange.keyword, userTimezone).catch(() => undefined);

    if (!actualTimeRange) {
      errors = { keyword: 'Unable to parse keyword' };

      return errors;
    }

    if (exceedsDuration(actualTimeRange.from, actualTimeRange.to, limitDuration)) {
      errors = { keyword: rangeLimitError };

      return errors;
    }
  }

  return errors;
};

const validateTimeRange = (
  timeRange: TimeRange | NoTimeRangeOverride,
  limitDuration: number,
  userTimezone: string,
  testKeyword = true,
) => {
  if (isTypeKeyword(timeRange)) {
    return validateKeywordTimeRange(timeRange, limitDuration, userTimezone, testKeyword);
  }

  if (isTypeRelativeWithEnd(timeRange)) {
    return validateRelativeTimeRangeWithEnd(timeRange, limitDuration);
  }

  if (isTypeAbsolute(timeRange)) {
    return validateAbsoluteTimeRange(timeRange, limitDuration);
  }

  return {};
};

export default validateTimeRange;
