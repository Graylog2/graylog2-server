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
import { render, screen } from 'wrappedTestingLibrary';
import { Formik, Form } from 'formik';

import AbsoluteCalendar from './AbsoluteCalendar';

const defaultProps = {
  disabled: false,
  currentTimeRange: {
    type: 'absolute',
    from: '1955-05-11 06:15:00.000',
    to: '1985-10-25 08:18:00.000',
  },
} as const;

const renderWithForm = (element) => render((
  <Formik initialValues={{ nextTimeRange: defaultProps.currentTimeRange, timerange: defaultProps.currentTimeRange }}
          onSubmit={() => {}}>
    <Form>
      {element}
    </Form>
  </Formik>
));

describe('AbsoluteCalendar', () => {
  it('renders `from` date', () => {
    renderWithForm(<AbsoluteCalendar {...defaultProps} range="from" />);

    const monthYear = screen.getByText('May 1955');
    const inputHour = screen.getByRole('spinbutton', { name: /from hour/i });
    const inputMinute = screen.getByRole('spinbutton', { name: /from minutes/i });
    const inputSeconds = screen.getByRole('spinbutton', { name: /from seconds/i });
    const inputMS = screen.getByRole('spinbutton', { name: /from milliseconds/i });

    expect(monthYear).not.toBeNull();
    // @ts-ignore
    expect(inputHour.value).toBe('06');
    // @ts-ignore
    expect(inputMinute.value).toBe('15');
    // @ts-ignore
    expect(inputSeconds.value).toBe('00');
    // @ts-ignore
    expect(inputMS.value).toBe('000');
  });

  it('renders `to` date', () => {
    renderWithForm(<AbsoluteCalendar {...defaultProps} range="to" startDate={new Date(defaultProps.currentTimeRange.from)} />);

    const monthYear = screen.getByText('October 1985');
    const inputHour = screen.getByRole('spinbutton', { name: /to hour/i });
    const inputMinute = screen.getByRole('spinbutton', { name: /to minutes/i });
    const inputSeconds = screen.getByRole('spinbutton', { name: /to seconds/i });
    const inputMS = screen.getByRole('spinbutton', { name: /to milliseconds/i });

    expect(monthYear).not.toBeNull();
    // @ts-ignore
    expect(inputHour.value).toBe('08');
    // @ts-ignore
    expect(inputMinute.value).toBe('18');
    // @ts-ignore
    expect(inputSeconds.value).toBe('00');
    // @ts-ignore
    expect(inputMS.value).toBe('000');
  });
});
