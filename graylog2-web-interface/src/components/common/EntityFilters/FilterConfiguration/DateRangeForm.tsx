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
import * as React from 'react';
import { useCallback, useMemo } from 'react';
import styled, { css } from 'styled-components';
import { Formik, Form } from 'formik';

import useUserDateTime from 'hooks/useUserDateTime';
import { ModalSubmit } from 'components/common';
import { filterValueTitle } from 'components/common/EntityFilters/helpers/timeRange';
import parseTimerangeFilter, { timeRangeToFilterValue } from 'components/common/PaginatedEntityTable/parseTimerangeFilter';
import TimeRangeTabs from 'views/components/time-range-picker/TimeRangePickerTabs';
import type { TimeRangePickerFormValues } from 'views/components/time-range-picker/TimeRangePicker';
import { normalizeFromPickerForSearchBar } from 'views/logic/queries/NormalizeTimeRange';
import { classifyRelativeTimeRange, normalizeIfClassifiedRelativeTimeRange } from 'views/components/time-range-picker/RelativeTimeRangeClassifiedHelper';
import validateTimeRange from 'views/components/TimeRangeValidation';
import { isTimeRange, isTypeRelative } from 'views/typeGuards/timeRange';
import type { DateTime, DateTimeFormats } from 'util/DateTime';
import { toDateObject } from 'util/DateTime';

import type { Filter } from '../types';

const Container = styled.div`
  padding: 3px 10px;
`;

const Info = styled.p(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.small};
    margin: 0 0 10px;
  `,
);

const defaultAbsoluteRange = (formatTime: (time: DateTime, format?: DateTimeFormats) => string) => ({
  type: 'absolute' as const,
  from: formatTime(toDateObject(new Date()).subtract(300, 'seconds'), 'complete'),
  to: formatTime(toDateObject(new Date()), 'complete'),
});

const buildInitialValues = (
  filter: Filter | undefined,
  formatTime: (time: DateTime, format?: DateTimeFormats) => string,
): TimeRangePickerFormValues => {
  if (!filter) {
    const absolute = defaultAbsoluteRange(formatTime);

    return { timeRangeTabs: { absolute }, activeTab: 'absolute' };
  }

  const timeRange = parseTimerangeFilter(filter.value);

  if (!timeRange) {
    const absolute = defaultAbsoluteRange(formatTime);

    return { timeRangeTabs: { absolute }, activeTab: 'absolute' };
  }

  const pickerValue = isTypeRelative(timeRange) ? classifyRelativeTimeRange(timeRange) : timeRange;

  return {
    timeRangeTabs: { [timeRange.type]: pickerValue },
    activeTab: timeRange.type,
  };
};

const dateTimeValidate = async (
  { timeRangeTabs, activeTab }: TimeRangePickerFormValues,
  formatTime: (dateTime: DateTime, format: string) => string,
  userTimezone: string,
) => {
  const activeTabTimeRange = timeRangeTabs[activeTab];

  if (!activeTabTimeRange) return {};

  const normalized = normalizeIfClassifiedRelativeTimeRange(activeTabTimeRange);
  const errors = await validateTimeRange(normalized, 0, formatTime, userTimezone);

  return Object.keys(errors).length !== 0 ? { timeRangeTabs: { [activeTabTimeRange.type]: errors } } : {};
};

type Props = {
  onSubmit: (filter: { title: string; value: string }) => void;
  filter: Filter | undefined;
};

const DateRangeForm = ({ filter, onSubmit }: Props) => {
  const { formatTime, userTimezone } = useUserDateTime();
  const initialValues = useMemo(() => buildInitialValues(filter, formatTime), [filter, formatTime]);

  const _onSubmit = useCallback(
    ({ timeRangeTabs, activeTab }: TimeRangePickerFormValues) => {
      const timeRange = normalizeFromPickerForSearchBar(timeRangeTabs[activeTab]);

      if (!isTimeRange(timeRange)) return;

      const value = timeRangeToFilterValue(timeRange);
      onSubmit({ title: filterValueTitle(value), value });
    },
    [onSubmit],
  );

  const _validate = useCallback(
    (values: TimeRangePickerFormValues) => dateTimeValidate(values, formatTime, userTimezone),
    [formatTime, userTimezone],
  );

  return (
    <Container data-testid="time-range-form">
      <Formik<TimeRangePickerFormValues>
        initialValues={initialValues}
        onSubmit={_onSubmit}
        validate={_validate}
        validateOnMount>
        {({ isValid, isValidating }) => (
          <Form>
            <TimeRangeTabs limitDuration={0} validTypes={['absolute', 'relative', 'keyword']} />
            <Info>
              All timezones using: <b>{userTimezone}</b>
            </Info>
            <ModalSubmit
              submitButtonText={`${filter ? 'Update' : 'Create'} filter`}
              bsSize="small"
              disabledSubmit={!isValid || isValidating}
              displayCancel={false}
            />
          </Form>
        )}
      </Formik>
    </Container>
  );
};

export default DateRangeForm;
