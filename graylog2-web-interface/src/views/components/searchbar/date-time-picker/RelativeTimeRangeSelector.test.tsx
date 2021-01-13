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
import { fireEvent, render, screen } from 'wrappedTestingLibrary';
import { Formik, Form } from 'formik';

import type { TimeRange } from 'views/logic/queries/Query';

import OriginalRelativeTimeRangeSelector from './RelativeTimeRangeSelector';

const defaultProps = {
  limitDuration: 0,
  disabled: false,
  originalTimeRange: {
    type: 'relative',
    range: 3600,
  },
} as const;

const initialValues = {
  nextTimeRange: defaultProps.originalTimeRange,
};

type Props = {
  disabled: boolean,
  limitDuration: number,
  originalTimeRange: TimeRange,
};

const RelativeTimeRangeSelector = (allProps: Props) => (
  <Formik initialValues={initialValues}
          onSubmit={() => {}}
          validateOnMount>
    <Form>
      <OriginalRelativeTimeRangeSelector {...allProps} />
    </Form>
  </Formik>
);

describe('RelativeTimeRangeSelector', () => {
  it('renders originalTimeRange value', () => {
    render(<RelativeTimeRangeSelector {...defaultProps} />);

    const spinbutton = screen.getByRole('spinbutton', { name: /set the range start value/i });

    expect(spinbutton).toBeInTheDocument();
    expect(spinbutton).toHaveValue(1);
  });

  it('renders originalTimeRange type', () => {
    render(<RelativeTimeRangeSelector {...defaultProps} />);

    expect(screen.getByText(/Hours/i)).toBeInTheDocument();
  });

  it('Clicking All Time disables input', () => {
    render(<RelativeTimeRangeSelector {...defaultProps} />);

    const allTimeCheckbox = screen.getByRole('checkbox', { name: /All Time/i });
    const rangeValue = screen.getByRole('spinbutton', { name: /set the range start value/i });

    expect(rangeValue).not.toBeDisabled();

    fireEvent.click(allTimeCheckbox);

    expect(rangeValue).toBeDisabled();
  });

  it('All Time checkbox is disabled', () => {
    render(<RelativeTimeRangeSelector {...defaultProps} limitDuration={10} />);

    const allTimeCheckbox = screen.getByRole('checkbox', { name: /All Time/i });

    expect(allTimeCheckbox).toBeDisabled();
  });
});
