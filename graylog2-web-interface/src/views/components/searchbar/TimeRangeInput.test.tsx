import * as React from 'react';
import { fireEvent, render, screen, waitFor, within } from 'wrappedTestingLibrary';

import TimeRangeInput from 'views/components/searchbar/TimeRangeInput';

describe('LogViewExportSettings', () => {
  const defaultTimeRange = { type: 'relative', range: 300 };

  it('opens date picker dropdown when clicking button', async () => {
    render(<TimeRangeInput currentTimeRange={defaultTimeRange} setCurrentTimeRange={() => {}} />);

    const button = await screen.findByRole('button', {
      name: /open time range selector/i,
      hidden: true,
    });

    fireEvent.click(button);

    await screen.findByText(/Search Time Range/);
  });

  it('displays relative time range of 5 minutes', async () => {
    render(<TimeRangeInput currentTimeRange={defaultTimeRange} setCurrentTimeRange={() => {}} />);

    const from = await screen.findByTestId('from');
    await within(from).findByText(/5 minutes ago/i);

    const to = await screen.findByTestId('to');
    await within(to).findByText(/now/i);
  });

  it('opens date picker dropdown when clicking summary', async () => {
    render(<TimeRangeInput currentTimeRange={defaultTimeRange} setCurrentTimeRange={() => {}} />);

    fireEvent.click(await screen.findByText(/5 minutes ago/));

    await screen.findByText(/Search Time Range/);
  });

  it('calls callback when changing time range', async () => {
    const onChange = jest.fn();
    render(<TimeRangeInput currentTimeRange={defaultTimeRange} setCurrentTimeRange={onChange} />);

    fireEvent.click(await screen.findByText(/5 minutes ago/));

    await screen.findByText(/Search Time Range/);

    const fromValue = await screen.findByRole('spinbutton', {
      name: /set the from value/i,
    });
    fireEvent.change(fromValue, { target: { value: 30 } });

    fireEvent.click(await screen.findByRole('button', { name: 'Apply' }));

    await waitFor(() => expect(onChange).toHaveBeenCalledWith({
      from: 1800,
      type: 'relative',
    }));
  });
});
