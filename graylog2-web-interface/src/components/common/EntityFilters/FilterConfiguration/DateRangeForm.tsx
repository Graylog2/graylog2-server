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
import styled from 'styled-components';
import { Formik, Form } from 'formik';

import ModalSubmit from 'components/common/ModalSubmit';
import loadAsync from 'routing/loadAsync';
import useUserDateTime from 'hooks/useUserDateTime';
import type { TimeRangePickerFormValues } from 'views/components/time-range-picker/TimeRangePicker';
import {
  classifyFromRange,
  classifyToRange,
  RELATIVE_CLASSIFIED_ALL_TIME_RANGE,
} from 'views/components/time-range-picker/RelativeTimeRangeClassifiedHelper';
import type { AbsoluteTimeRange, RelativeTimeRange, TimeRange } from 'views/logic/queries/Query';
import { isTypeRelativeWithStartOnly } from 'views/typeGuards/timeRange';
import type { DateTime, DateTimeFormats } from 'util/DateTime';

import { parseFilterValue, timeRangePickerFormValuesToFilterValue } from '../helpers/timeRange';
import type { Filter } from '../types';

const TimeRangePickerFormContent = loadAsync(
  () => import('views/components/time-range-picker/TimeRangePickerFormContent'),
);

const Container = styled.div`
  padding: 3px 10px;
  width: 735px;
`;

type FormatTime = (dateTime: DateTime, format?: DateTimeFormats) => string;

const defaultInitialValues = (): TimeRangePickerFormValues => ({
  timeRangeTabs: {
    relative: {
      type: 'relative',
      from: {
        value: 5,
        unit: 'minutes',
        isAllTime: false,
      },
      to: RELATIVE_CLASSIFIED_ALL_TIME_RANGE,
    },
  },
  activeTab: 'relative',
});

const relativeTimeRangeToFormValue = (timeRange: RelativeTimeRange) => {
  if (isTypeRelativeWithStartOnly(timeRange)) {
    return {
      type: 'relative' as const,
      from: classifyFromRange(timeRange.range),
      to: RELATIVE_CLASSIFIED_ALL_TIME_RANGE,
    };
  }

  return {
    type: 'relative' as const,
    from: classifyFromRange(timeRange.from),
    to: typeof timeRange.to === 'number' ? classifyToRange(timeRange.to) : RELATIVE_CLASSIFIED_ALL_TIME_RANGE,
  };
};

const absoluteTimeRangeToFormValue = (timeRange: AbsoluteTimeRange, formatTime: FormatTime) => ({
  type: 'absolute' as const,
  from: timeRange.from ? formatTime(timeRange.from, 'complete') : '',
  to: timeRange.to ? formatTime(timeRange.to, 'complete') : formatTime(new Date(), 'complete'),
});

const timeRangeToFormValues = (timeRange: TimeRange, formatTime: FormatTime): TimeRangePickerFormValues => {
  switch (timeRange.type) {
    case 'relative':
      return {
        timeRangeTabs: {
          relative: relativeTimeRangeToFormValue(timeRange),
        },
        activeTab: 'relative',
      };
    case 'keyword':
      return {
        timeRangeTabs: {
          keyword: timeRange,
        },
        activeTab: 'keyword',
      };
    case 'absolute':
      return {
        timeRangeTabs: {
          absolute: absoluteTimeRangeToFormValue(timeRange, formatTime),
        },
        activeTab: 'absolute',
      };
    default:
      throw new Error(`Invalid time range type: ${timeRange}`);
  }
};

export const filterValueToTimeRangePickerFormValues = (filterValue: string | undefined, formatTime: FormatTime) => {
  if (!filterValue) {
    return defaultInitialValues();
  }

  return timeRangeToFormValues(parseFilterValue(filterValue), formatTime);
};

type Props = {
  onSubmit: (filter: { title: string; value: string }) => void;
  filter: Filter | undefined;
};

const DateRangeForm = ({ filter, onSubmit }: Props) => {
  const { formatTime, userTimezone } = useUserDateTime();
  const initialValues = useMemo(
    () => filterValueToTimeRangePickerFormValues(filter?.value, formatTime),
    [filter?.value, formatTime],
  );

  const _onSubmit = useCallback(
    (values: TimeRangePickerFormValues) => {
      const serializedFilter = timeRangePickerFormValuesToFilterValue(values, userTimezone, formatTime);

      onSubmit(serializedFilter);
    },
    [formatTime, onSubmit, userTimezone],
  );

  return (
    <Container data-testid="time-range-form">
      <Formik<TimeRangePickerFormValues> initialValues={initialValues} onSubmit={_onSubmit} enableReinitialize>
        {({ isValid }) => (
          <Form>
            <TimeRangePickerFormContent limitDuration={0}>
              <ModalSubmit
                disabledSubmit={!isValid}
                submitButtonText={`${filter ? 'Update' : 'Create'} filter`}
                bsSize="small"
                displayCancel={false}
              />
            </TimeRangePickerFormContent>
          </Form>
        )}
      </Formik>
    </Container>
  );
};

export default DateRangeForm;
