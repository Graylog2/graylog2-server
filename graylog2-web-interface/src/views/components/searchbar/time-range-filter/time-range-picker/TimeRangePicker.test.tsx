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
import { fireEvent, render, screen, waitFor } from 'wrappedTestingLibrary';
import { applyTimeoutMultiplier } from 'jest-preset-graylog/lib/timeouts';
import userEvent from '@testing-library/user-event';
import { defaultUser } from 'defaultMockValues';

import { StoreMock as MockStore, asMock } from 'helpers/mocking';
import mockSearchClusterConfig from 'fixtures/searchClusterConfig';
import ToolsStore from 'stores/tools/ToolsStore';
import useCurrentUser from 'hooks/useCurrentUser';
import { adminUser } from 'fixtures/users';

import OriginalTimeRangePicker from './TimeRangePicker';

jest.mock('hooks/useCurrentUser');

jest.mock('views/stores/SearchConfigStore', () => ({
  SearchConfigActions: {
    refresh: jest.fn(() => Promise.resolve()),
  },
  SearchConfigStore: MockStore(
    'get',
    'refresh',
    ['getInitialState', () => ({ searchesClusterConfig: mockSearchClusterConfig })],
  ),
}));

jest.mock('stores/tools/ToolsStore', () => ({
  testNaturalDate: jest.fn(),
}));

const defaultProps = {
  show: true,
  currentTimeRange: {
    type: 'relative',
    from: 300,
  },
  limitDuration: 0,
  noOverride: false,
  setCurrentTimeRange: jest.fn(),
  toggleDropdownShow: jest.fn(),
  position: 'bottom',
} as const;

const TimeRangePicker = (props: React.ComponentProps<typeof OriginalTimeRangePicker>) => (
  <OriginalTimeRangePicker {...props}>
    <button type="button">Open</button>
  </OriginalTimeRangePicker>
);

describe('TimeRangePicker', () => {
  beforeEach(() => {
    asMock(ToolsStore.testNaturalDate).mockImplementation(() => Promise.resolve({
      from: '2018-11-14 13:52:38',
      to: '2018-11-14 13:57:38',
      timezone: 'Asia/Tokyo',
    }));

    asMock(useCurrentUser).mockReturnValue(defaultUser);
  });

  it('renders initial time range value', async () => {
    render(<TimeRangePicker {...defaultProps} />);

    const title = await screen.findByText(/search time range/i);

    expect(title).toBeInTheDocument();
  });

  it('Clicking apply run handler', async () => {
    render(<TimeRangePicker {...defaultProps} />);

    const applyButton = screen.getByRole('button', { name: /update time range/i });
    fireEvent.click(applyButton);

    await waitFor(() => expect(defaultProps.setCurrentTimeRange).toHaveBeenCalled());

    await waitFor(() => expect(defaultProps.setCurrentTimeRange).toHaveBeenCalledWith({
      type: 'relative',
      from: 300,
    }));
  });

  it('Limit duration is shown when setup', async () => {
    render(<TimeRangePicker {...defaultProps} limitDuration={259200} />);

    const limitDuration = await screen.findByText(/admin has limited searching to 3 days ago/i);

    expect(limitDuration).toBeInTheDocument();
  });

  it('Render Tabs', async () => {
    render(<TimeRangePicker {...defaultProps} />);

    const relativeTabButton = await screen.findByRole('tab', { name: /relative/i });
    const absoluteTabButton = screen.getByRole('tab', { name: /absolute/i });
    const keywordTabButton = screen.getByRole('tab', { name: /keyword/i });

    expect(relativeTabButton).toBeInTheDocument();
    expect(absoluteTabButton).toBeInTheDocument();
    expect(keywordTabButton).toBeInTheDocument();
  });

  it('Absolute tab has Accordion', async () => {
    render(<TimeRangePicker {...defaultProps} currentTimeRange={{ type: 'absolute', from: '1955-05-11 06:15:00', to: '1985-10-25 08:18:00' }} />);

    const calendarButton = await screen.findByRole('button', { name: /calendar/i });
    const timestampButton = screen.getByRole('button', { name: /timestamp/i });

    expect(calendarButton).toBeInTheDocument();
    expect(timestampButton).toBeInTheDocument();

    fireEvent.click(timestampButton);

    const timestampContent = await screen.findByText(/Date should be formatted as/i);

    expect(timestampContent).toBeInTheDocument();
  }, applyTimeoutMultiplier(15000));

  it('Renders No Override Tab for Dashboard', async () => {
    render(<TimeRangePicker {...defaultProps} currentTimeRange={{}} noOverride />);

    const noOverrideContent = await screen.findByText(/No Date\/Time Override chosen./i);
    const noOverrideButton = screen.getByRole('button', { name: /no override/i });

    expect(noOverrideButton).toBeInTheDocument();
    expect(noOverrideContent).toBeInTheDocument();
  });

  it('Should not change keyword time range after submitting without changes', async () => {
    const setCurrentTimeRange = jest.fn();

    render(<TimeRangePicker {...defaultProps}
                            currentTimeRange={{ type: 'keyword', keyword: 'yesterday', timezone: 'Asia/Tokyo' }}
                            setCurrentTimeRange={setCurrentTimeRange} />);

    const submitButton = await screen.findByRole('button', {
      name: /update time range/i,
    });

    userEvent.click(submitButton);

    await waitFor(() => expect(setCurrentTimeRange).toHaveBeenCalledTimes(1));

    expect(setCurrentTimeRange).toHaveBeenCalledWith({ type: 'keyword', keyword: 'yesterday', timezone: 'Asia/Tokyo' });
  });

  it('Displays button for admin users to save time range as preset', async () => {
    asMock(useCurrentUser).mockReturnValue(adminUser);

    render(<TimeRangePicker {...defaultProps} />);

    await screen.findByTitle('Save current time range as preset');
  });
});
