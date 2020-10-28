// @flow strict
import React from 'react';
import { fireEvent, render, within, screen } from 'wrappedTestingLibrary';
import { Formik, Form } from 'formik';

import OriginalRelativeTimeRangeSelector from './RelativeTimeRangeSelector';

const defaultProps = {
  config: {
    query_time_range_limit: '0',
  },
  disabled: false,
  originalTimeRange: {
    type: 'relative',
    range: 3600,
  },
};

type Props = {
  disabled: boolean,
  config: {
    query_time_range_limit: string,
  },
  originalTimeRange: {
    range: string | number,
  },
};

const RelativeTimeRangeSelector = (allProps: Props) => (
  <Formik initialValues={{}}
          onSubmit={() => {}}
          validateOnMount>
    <Form>
      <OriginalRelativeTimeRangeSelector {...allProps} />
    </Form>
  </Formik>
);

describe('RelativeTimeRangeSelector', () => {
  it('renders originalTimeRange value', () => {
    const { getByRole } = render(<RelativeTimeRangeSelector {...defaultProps} />);

    expect(getByRole('spinbutton', { name: /set the range value/i }).value).toBe('1');
  });

  it('renders originalTimeRange type', () => {
    const { container } = render(<RelativeTimeRangeSelector {...defaultProps} />);
    const { getByText } = within(container.querySelector('#relative-timerange-from-length > div'));

    expect(getByText('Hours')).toBeTruthy();
  });

  it('Clicking All Time disables input', () => {
    render(<RelativeTimeRangeSelector {...defaultProps} />);

    const allTimeCheckbox = screen.getByRole('checkbox', { name: /All Time/i });
    const rangeValue = screen.getByRole('spinbutton', { name: /set the range value/i });

    expect(rangeValue).not.toBeDisabled();

    fireEvent.click(allTimeCheckbox);

    expect(rangeValue).toBeDisabled();
  });

  it('All Time checkbox is disabled * notice shown if timerange limit is set', () => {
    render(<RelativeTimeRangeSelector {...defaultProps} config={{ query_time_range_limit: 'PT24H' }} />);

    const allTimeCheckbox = screen.getByRole('checkbox', { name: /All Time/i });
    const limitWarning = screen.getByText(/Admin has limited searching to a day ago/i);

    expect(allTimeCheckbox).toBeDisabled();
    expect(limitWarning).not.toBeNull();
  });
});
