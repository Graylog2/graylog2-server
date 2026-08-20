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
import type { TimeRangePickerFormValues } from 'views/components/time-range-picker/types';
import useTimeRangeValidation from 'views/components/time-range-picker/useTimeRangeValidation';

import { filterValueToTimeRangePickerFormValues, timeRangePickerFormValuesToFilterValue } from '../helpers/timeRange';
import type { Filter } from '../types';

const TimeRangePickerFormContent = loadAsync(
  () => import('views/components/time-range-picker/TimeRangePickerFormContent'),
);

const Container = styled.div`
  padding: 3px 10px;
  width: 735px;
`;

type Props = {
  onSubmit: (filter: { title: string; value: string }) => void;
  filter: Filter | undefined;
};

const DateRangeForm = ({ filter, onSubmit }: Props) => {
  const { formatTime, userTimezone } = useUserDateTime();
  const validate = useTimeRangeValidation();
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
      <Formik<TimeRangePickerFormValues>
        initialValues={initialValues}
        onSubmit={_onSubmit}
        enableReinitialize
        validate={validate}>
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
