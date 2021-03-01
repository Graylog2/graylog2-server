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

  it('shows "No Override" if no time range is provided', async () => {
    render(<TimeRangeInput currentTimeRange={{}} setCurrentTimeRange={() => {}} />);

    await screen.findByText('No Override');
  });
});
