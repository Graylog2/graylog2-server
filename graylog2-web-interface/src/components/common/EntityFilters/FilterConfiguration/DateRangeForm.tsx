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
import styled from 'styled-components';
import { Formik } from 'formik';

import TimeRangePickerFormContent from 'views/components/time-range-picker/TimeRangePickerFormContent';
import type { TimeRangePickerFormValues } from 'views/components/time-range-picker/TimeRangePicker';

import type { Filter } from '../types';

const Container = styled.div`
  padding: 3px 10px;
  width: 735px;
`;

const mockedInitialValues: TimeRangePickerFormValues = {
  timeRangeTabs: {
    relative: {
      type: 'relative',
      from: {
        value: 5,
        unit: 'minutes',
        isAllTime: false,
      },
      to: {
        value: 0,
        unit: 'seconds',
        isAllTime: true,
      },
    },
  },
  activeTab: 'relative',
};

type Props = {
  onSubmit: (filter: { title: string; value: string }) => void;
  filter: Filter | undefined;
};

const DateRangeForm = (_props: Props) => (
  <Container data-testid="time-range-form">
    <Formik<TimeRangePickerFormValues> initialValues={mockedInitialValues} onSubmit={() => {}}>
      {() => <TimeRangePickerFormContent limitDuration={0} />}
    </Formik>
  </Container>
);

export default DateRangeForm;
