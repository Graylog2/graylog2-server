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

import OriginalTabRelativeTimeRange from './TabRelativeTimeRange';

const defaultProps = {
  limitDuration: 0,
  disabled: false,
  originalTimeRange: {
    type: 'relative',
    range: 3600,
  },
};

const initialValues = {
  nextTimeRange: defaultProps.originalTimeRange,
};

type Props = {
  disabled: boolean,
  limitDuration: number,
  originalTimeRange: {
    range: string | number,
  },
};

const TabRelativeTimeRange = (allProps: Props) => (
  <Formik initialValues={initialValues}
          onSubmit={() => {}}
          validateOnMount>
    <Form>
      <OriginalTabRelativeTimeRange {...allProps} />
    </Form>
  </Formik>
);

describe('TabRelativeTimeRange', () => {
  it('renders originalTimeRange value', () => {
    render(<TabRelativeTimeRange {...defaultProps} />);

    const spinbutton = screen.getByRole('spinbutton', { name: /set the range value/i });

    expect(spinbutton).toBeInTheDocument();
    expect(spinbutton).toHaveValue(1);
  });

  it('renders originalTimeRange type', () => {
    render(<TabRelativeTimeRange {...defaultProps} />);

    expect(screen.getByText(/Hours/i)).toBeInTheDocument();
  });

  it('Clicking All Time disables input', () => {
    render(<TabRelativeTimeRange {...defaultProps} />);

    const allTimeCheckbox = screen.getByRole('checkbox', { name: /All Time/i });
    const rangeValue = screen.getByRole('spinbutton', { name: /set the range value/i });

    expect(rangeValue).not.toBeDisabled();

    fireEvent.click(allTimeCheckbox);

    expect(rangeValue).toBeDisabled();
  });

  it('All Time checkbox is disabled', () => {
    render(<TabRelativeTimeRange {...defaultProps} limitDuration={10} />);

    const allTimeCheckbox = screen.getByRole('checkbox', { name: /All Time/i });

    expect(allTimeCheckbox).toBeDisabled();
  });
});
