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
import type { AbsoluteTimeRange, KeywordTimeRange, RelativeTimeRange, TimeRange } from 'views/logic/queries/Query';
import { isTypeRelativeWithStartOnly } from 'views/typeGuards/timeRange';
import type { DateTime, DateTimeFormats } from 'util/DateTime';

import {
  DATE_SEPARATOR,
  TIME_RANGE_TYPE_SEPARATOR,
  timeRangePickerFormValuesToFilterValue,
} from '../helpers/timeRange';
import type { Filter } from '../types';

const TimeRangePickerFormContent = loadAsync(
  () => import('views/components/time-range-picker/TimeRangePickerFormContent'),
);

const Container = styled.div`
  padding: 3px 10px;
  width: 735px;
`;

type FormatTime = (dateTime: DateTime, format?: DateTimeFormats) => string;

const decodeKeywordValue = (value: string) => decodeURIComponent(value.replace(/\+/g, '%20'));
const decodeAbsoluteValue = (value: string) => decodeURIComponent(value);

const isNumericRangeValue = (value: string | undefined) =>
  value !== undefined && value !== '' && Number.isFinite(Number(value));

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

const parseRelativeTimeRange = (value: string): RelativeTimeRange => {
  const [from, to] = value.split(DATE_SEPARATOR);
  const fromRange = Number(from);

  if (!Number.isFinite(fromRange)) {
    throw new Error(`Invalid relative time range value: ${value}`);
  }

  if (to !== undefined && to !== '') {
    const toRange = Number(to);

    return {
      type: 'relative',
      from: fromRange,
      ...(Number.isFinite(toRange) && { to: toRange }),
    };
  }

  return {
    type: 'relative',
    range: fromRange,
  };
};

const parseAbsoluteTimeRange = (value: string): AbsoluteTimeRange => {
  const [from, to = ''] = value.split(DATE_SEPARATOR);

  return {
    type: 'absolute',
    from: decodeAbsoluteValue(from),
    to: decodeAbsoluteValue(to),
  };
};

const parseUntypedTimeRange = (value: string): TimeRange => {
  const ranges = value.split(DATE_SEPARATOR);
  const isRelative = ranges.every((range) => range === '' || isNumericRangeValue(range));

  return isRelative ? parseRelativeTimeRange(value) : parseAbsoluteTimeRange(value);
};

const parseFilterValue = (filterValue: string): TimeRange => {
  const separatorIndex = filterValue.indexOf(TIME_RANGE_TYPE_SEPARATOR);

  if (separatorIndex < 0) {
    return parseUntypedTimeRange(filterValue);
  }

  const type = filterValue.slice(0, separatorIndex);
  const value = filterValue.slice(separatorIndex + TIME_RANGE_TYPE_SEPARATOR.length);

  switch (type) {
    case 'relative':
      return parseRelativeTimeRange(value);
    case 'keyword':
      return {
        type,
        keyword: decodeKeywordValue(value),
      } satisfies KeywordTimeRange;
    case 'absolute':
      return parseAbsoluteTimeRange(value);
    default:
      return parseUntypedTimeRange(filterValue);
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
        {() => (
          <Form>
            <TimeRangePickerFormContent limitDuration={0}>
              <ModalSubmit
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
