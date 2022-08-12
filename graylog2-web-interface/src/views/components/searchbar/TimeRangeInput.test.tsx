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
import { Formik } from 'formik';

import MockStore from 'helpers/mocking/StoreMock';
import MockAction from 'helpers/mocking/MockAction';
import TimeRangeInput from 'views/components/searchbar/TimeRangeInput';

jest.mock('stores/configurations/ConfigurationsStore', () => ({
  ConfigurationsStore: MockStore(),
  ConfigurationsActions: {
    listSearchesClusterConfig: MockAction(),
  },
}));

describe('TimeRangeInput', () => {
  const defaultTimeRange = { type: 'relative', range: 300 };

  const SUTTimeRangeInput = (props) => (
    <Formik initialValues={{ selectedFields: [] }} onSubmit={() => {}}>
      <TimeRangeInput value={defaultTimeRange} onChange={() => {}} {...props} />
    </Formik>
  );

  it('opens date picker dropdown when clicking button', async () => {
    render(<SUTTimeRangeInput />);

    const button = await screen.findByRole('button', {
      name: /open time range selector/i,
      hidden: true,
    });

    fireEvent.click(button);

    await screen.findByText(/Search Time Range/);
  });

  it('displays relative time range of 5 minutes', async () => {
    render(<SUTTimeRangeInput />);

    const from = await screen.findByTestId('from');
    await within(from).findByText(/5 minutes ago/i);

    const to = await screen.findByTestId('to');
    await within(to).findByText(/now/i);
  });

  it('opens date picker dropdown when clicking summary', async () => {
    render(<SUTTimeRangeInput />);

    fireEvent.click(await screen.findByText(/5 minutes ago/));

    await screen.findByText(/Search Time Range/);
  });

  it('calls callback when changing time range', async () => {
    const onChange = jest.fn();
    render(<SUTTimeRangeInput value={defaultTimeRange} onChange={onChange} />);

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
    render(<SUTTimeRangeInput value={{}} onChange={() => {}} />);

    await screen.findByText('No Override');
  });

  it('shows all tabs if no `validTypes` prop is passed', async () => {
    render(<SUTTimeRangeInput onChange={() => {}} value={defaultTimeRange} />);

    fireEvent.click(await screen.findByText(/5 minutes ago/));

    await screen.findByRole('tab', { name: 'Absolute' });
    await screen.findByRole('tab', { name: 'Keyword' });
    await screen.findByRole('tab', { name: 'Relative' });
  });

  it('shows only valid tabs if `validTypes` prop is passed', async () => {
    render(<SUTTimeRangeInput onChange={() => {}} value={defaultTimeRange} validTypes={['relative']} />);

    fireEvent.click(await screen.findByText(/5 minutes ago/));

    await screen.findByRole('tab', { name: 'Relative' });

    expect(screen.queryByRole('tab', { name: 'Keyword' })).not.toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: 'Absolute' })).not.toBeInTheDocument();
  });

  it('shows a dropdown button allowing to quick-select presets', async () => {
    render(<SUTTimeRangeInput onChange={() => {}} value={defaultTimeRange} validTypes={['relative']} />);

    const dropdown = await screen.findByRole('button', { name: /open time range preset select/i });

    fireEvent.click(dropdown);

    await screen.findByRole('heading', { name: 'From (Until Now)' });
  });

  it('allows hiding the dropdown button for quick-selecting presets', async () => {
    render(<SUTTimeRangeInput onChange={() => {}} value={defaultTimeRange} validTypes={['relative']} showPresetDropdown={false} />);

    await screen.findByText(/5 minutes ago/);

    expect(screen.queryByRole('button', { name: /open time range preset select/i })).not.toBeInTheDocument();
  });
});
