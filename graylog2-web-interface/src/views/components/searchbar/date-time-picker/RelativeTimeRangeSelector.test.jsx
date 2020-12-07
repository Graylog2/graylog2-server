// @flow strict
import React from 'react';
import { fireEvent, render, screen } from 'wrappedTestingLibrary';
import { Formik, Form } from 'formik';

import OriginalRelativeTimeRangeSelector from './RelativeTimeRangeSelector';

const defaultProps = {
  limitDuration: 0,
  disabled: false,
  originalTimeRange: {
    type: 'relative',
    range: 3600,
  },
};

const initialValues = {
  tempTimeRange: defaultProps.originalTimeRange,
};

type Props = {
  disabled: boolean,
  limitDuration: number,
  originalTimeRange: {
    range: string | number,
  },
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

    const spinbutton = screen.getByRole('spinbutton', { name: /set the range value/i });

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
    const rangeValue = screen.getByRole('spinbutton', { name: /set the range value/i });

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
