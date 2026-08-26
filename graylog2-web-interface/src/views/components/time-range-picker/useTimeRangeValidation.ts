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
import { useCallback } from 'react';

import useUserDateTime from 'hooks/useUserDateTime';
import type { DateTime } from 'util/DateTime';
import validateTimeRange from 'views/components/TimeRangeValidation';

import { normalizeIfClassifiedRelativeTimeRange } from './RelativeTimeRangeClassifiedHelper';
import type { TimeRangePickerFormValues, TimeRangePickerTimeRange } from './types';

const dateTimeValidate = async (
  activeTabTimeRange: TimeRangePickerTimeRange,
  limitDuration: number,
  formatTime: (dateTime: DateTime, format: string) => string,
  userTimezone: string,
) => {
  if (!activeTabTimeRange) {
    return {};
  }

  const normalizedTimeRange = normalizeIfClassifiedRelativeTimeRange(activeTabTimeRange);
  const timeRangeErrors = await validateTimeRange(normalizedTimeRange, limitDuration, formatTime, userTimezone);

  return Object.keys(timeRangeErrors).length !== 0
    ? { timeRangeTabs: { [activeTabTimeRange.type]: timeRangeErrors } }
    : {};
};

const useTimeRangeValidation = (limitDuration: number = 0) => {
  const { formatTime, userTimezone } = useUserDateTime();

  return useCallback(
    ({ timeRangeTabs, activeTab }: TimeRangePickerFormValues) =>
      dateTimeValidate(activeTab ? timeRangeTabs[activeTab] : undefined, limitDuration, formatTime, userTimezone),
    [formatTime, limitDuration, userTimezone],
  );
};

export default useTimeRangeValidation;
