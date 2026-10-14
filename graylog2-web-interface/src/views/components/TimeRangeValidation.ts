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
import i18n from 'i18n';
import { isTypeAbsolute, isTypeRelativeWithEnd, isTypeKeyword } from 'views/typeGuards/timeRange';
import type { DateTime } from 'util/DateTime';
import { isValidDate, toDateObject } from 'util/DateTime';
import debounceWithPromise from 'views/logic/debounceWithPromise';
import { testNaturalDate } from 'api/tools';

const invalidDateFormatError = () =>
  i18n.t('common_views.time_range_validation.invalid_date_format', {
    ns: 'search',
    format: 'YYYY-MM-DD [HH:mm:ss[.SSS]]',
  });
const rangeLimitError = () => i18n.t('common_views.time_range_validation.range_limit', { ns: 'search' });
const dateLimitError = () => i18n.t('common_views.time_range_validation.date_limit', { ns: 'search' });
const timeRangeError = () => i18n.t('common_views.time_range_validation.until_before_from', { ns: 'search' });
const emptyError = () => i18n.t('common_views.time_range_validation.empty', { ns: 'search' });

const exceedsDuration = (
  dateTime: DateTime,
  limitDuration: number,
  formatTime: (dateTime: DateTime, format: string) => string,
) => {
  if (limitDuration === 0) {
    return false;
  }

  const durationLimit = formatTime(toDateObject(new Date()).subtract(Number(limitDuration), 'seconds'), 'complete');

  return moment(dateTime).isBefore(durationLimit);
};

const validateAbsoluteTimeRange = (
  timeRange: AbsoluteTimeRange,
  limitDuration: number,
  formatTime: (dateTime: DateTime, format: string) => string,
) => {
  let errors: {
    from?: string;
    to?: string;
  } = {};

  if (!isValidDate(timeRange.from)) {
    errors = { ...errors, from: invalidDateFormatError() };
  }

  if (!isValidDate(timeRange.to)) {
    errors = { ...errors, to: invalidDateFormatError() };
  }

  if (timeRange.from >= timeRange.to) {
    errors = { ...errors, to: timeRangeError() };
  }

  if (exceedsDuration(timeRange.from, limitDuration, formatTime)) {
    errors = { ...errors, from: dateLimitError() };
  }

  return errors;
};

const validateRelativeTimeRangeWithEnd = (timeRange: RelativeTimeRangeWithEnd, limitDuration: number) => {
  let errors: { from?: string; to?: string } = {};

  if (limitDuration > 0) {
    if (timeRange.from > limitDuration || !timeRange.from) {
      errors = { ...errors, from: rangeLimitError() };
    }

    if (timeRange.to > limitDuration) {
      errors = { ...errors, to: rangeLimitError() };
    }
  }

  if (timeRange.from === null) {
    errors = { ...errors, from: emptyError() };
  }

  if (timeRange.from && timeRange.to === null) {
    errors = { ...errors, to: emptyError() };
  }

  if (timeRange.from && timeRange.from <= timeRange.to) {
    errors = { ...errors, to: timeRangeError() };
  }

  return errors;
};

const debouncedTestNaturalDate = debounceWithPromise(testNaturalDate, 350);

const validateKeywordTimeRange = async (
  timeRange: KeywordTimeRange,
  limitDuration: number,
  formatTime: (dateTime: DateTime, format: string) => string,
  userTimezone: string,
  testKeyword: boolean,
) => {
  let errors: { keyword?: string } = {};

  if (trim(timeRange.keyword) === '') {
    errors = { keyword: i18n.t('common_views.time_range_validation.keyword_empty', { ns: 'search' }) };

    return errors;
  }

  if (testKeyword) {
    const actualTimeRange = await debouncedTestNaturalDate(timeRange.keyword, userTimezone).catch(() => undefined);

    if (!actualTimeRange) {
      errors = { keyword: i18n.t('common_views.time_range_validation.keyword_unparsable', { ns: 'search' }) };

      return errors;
    }

    if (exceedsDuration(formatTime(actualTimeRange.from, 'complete'), limitDuration, formatTime)) {
      errors = { keyword: rangeLimitError() };

      return errors;
    }
  }

  return errors;
};

const validateTimeRange = (
  timeRange: TimeRange | NoTimeRangeOverride,
  limitDuration: number,
  formatTime: (dateTime: DateTime, format: string) => string,
  userTimezone: string,
  testKeyword = true,
) => {
  if (isTypeKeyword(timeRange)) {
    return validateKeywordTimeRange(timeRange, limitDuration, formatTime, userTimezone, testKeyword);
  }

  if (isTypeRelativeWithEnd(timeRange)) {
    return validateRelativeTimeRangeWithEnd(timeRange, limitDuration);
  }

  if (isTypeAbsolute(timeRange)) {
    return validateAbsoluteTimeRange(timeRange, limitDuration, formatTime);
  }

  return {};
};

export default validateTimeRange;
