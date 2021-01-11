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
import React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import { Formik, Form } from 'formik';

import type { RelativeTimeRange, AbsoluteTimeRange, KeywordTimeRange, TimeRange } from 'views/logic/queries/Query';

import TimeRangeLivePreview from './TimeRangeLivePreview';

type Props = {
  timerange: TimeRange
};

const FormikWrap = ({ timerange }:Props) => (
  <Formik initialValues={{ nextTimeRange: timerange, timerange }}
          onSubmit={() => {}}
          validateOnMount>
    <Form>
      <TimeRangeLivePreview timerange={timerange} />
    </Form>
  </Formik>
);

describe('TimeRangeLivePreview', () => {
  it('renders relative timerange', () => {
    const timerange: RelativeTimeRange = { type: 'relative', range: 300 };
    render(<FormikWrap timerange={timerange} />);

    expect(screen.getByText(/5 minutes ago/i)).not.toBeNull();
  });

  it('renders absolute timerange', () => {
    const timerange: AbsoluteTimeRange = {
      type: 'absolute',
      from: '1955-11-05 06:15:00.000',
      to: '1985-10-25 08:18:00.000',
    };
    render(<FormikWrap timerange={timerange} />);

    expect(screen.getByText(/1955-11-05 06:15:00.000/i)).not.toBeNull();
    expect(screen.getByText(/1985-10-25 08:18:00.000/i)).not.toBeNull();
  });

  it('renders keyword timerange', () => {
    const timerange: KeywordTimeRange = { type: 'keyword', keyword: 'Last ten minutes', from: '2020-10-27 15:20:56', to: '2020-10-27 15:30:56' };
    render(<FormikWrap timerange={timerange} />);

    expect(screen.getByText(/2020-10-27 15:20:56/i)).not.toBeNull();
    expect(screen.getByText(/2020-10-27 15:30:56/i)).not.toBeNull();
  });
});
