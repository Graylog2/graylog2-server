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
import { StoreMock as MockStore } from 'helpers/mocking';
import { act } from 'react-dom/test-utils';
import mockSearchClusterConfig from 'fixtures/searchClusterConfig';

import ToolsStore from 'stores/tools/ToolsStore';

import { DateTimeContext } from './DateTimeProvider';
import OriginalTimeRangeDropDown, { TimeRangeDropdownProps } from './TimeRangeDropdown';

jest.mock('views/stores/SearchConfigStore', () => ({
  SearchConfigActions: {
    refresh: jest.fn(() => Promise.resolve()),
  },
  SearchConfigStore: MockStore(
    ['listen', () => jest.fn()],
    'get',
    ['getInitialState', () => ({ searchesClusterConfig: mockSearchClusterConfig })],
    ['refresh', () => jest.fn()],
  ),
}));

jest.mock('stores/tools/ToolsStore', () => ({}));

const defaultProps = {
  currentTimeRange: {
    type: 'relative',
    from: 300,
  },
  noOverride: false,
  setCurrentTimeRange: jest.fn(),
  toggleDropdownShow: jest.fn(),
  position: 'bottom',
} as const;

const TimeRangeDropdown = (allProps: TimeRangeDropdownProps) => (
  <DateTimeContext.Provider value={{
    limitDuration: 259200,
  }}>
    <OriginalTimeRangeDropDown {...allProps} />
  </DateTimeContext.Provider>

);

const asyncRender = async (element) => {
  let wrapper;

  await act(async () => { wrapper = render(element); });

  if (!wrapper) {
    throw new Error('Render returned `null`.');
  }

  return wrapper;
};

describe('TimeRangeDropdown', () => {
  beforeEach(() => {
    ToolsStore.testNaturalDate = jest.fn(() => Promise.resolve({
      from: '2018-11-14 13:52:38',
      to: '2018-11-14 13:57:38',
    }));
  });

  it('renders initial time range value', () => {
    render(<TimeRangeDropdown {...defaultProps} />);

    const title = screen.getByText(/search time range/i);

    expect(title).toBeInTheDocument();
  });

  it('Clicking apply run handler', async () => {
    render(<TimeRangeDropdown {...defaultProps} />);

    const applyButton = screen.getByRole('button', { name: /apply/i });
    fireEvent.click(applyButton);

    await waitFor(() => expect(defaultProps.setCurrentTimeRange).toHaveBeenCalled());

    await waitFor(() => expect(defaultProps.setCurrentTimeRange).toHaveBeenCalledWith({
      type: 'relative',
      from: 300,
    }));
  });

  it('Limit duration is shown when setup', () => {
    render(<TimeRangeDropdown {...defaultProps} />);
    const limitDuration = screen.getByText(/admin has limited searching to 3 days ago/i);

    expect(limitDuration).toBeInTheDocument();
  });

  it('Render Tabs', async () => {
    await asyncRender(<TimeRangeDropdown {...defaultProps} />);

    const relativeTabButton = screen.getByRole('tab', { name: /relative/i });
    const absoluteTabButton = screen.getByRole('tab', { name: /absolute/i });
    const keywordTabButton = screen.getByRole('tab', { name: /keyword/i });

    expect(relativeTabButton).toBeInTheDocument();
    expect(absoluteTabButton).toBeInTheDocument();
    expect(keywordTabButton).toBeInTheDocument();
  });

  it('Absolute tab has Accordion', async () => {
    await asyncRender(<TimeRangeDropdown {...defaultProps} currentTimeRange={{ type: 'absolute', from: '1955-05-11 06:15:00', to: '1985-10-25 08:18:00' }} />);

    const calendarButton = screen.getByRole('button', { name: /calendar/i });
    const timestampButton = screen.getByRole('button', { name: /timestamp/i });

    expect(calendarButton).toBeInTheDocument();
    expect(timestampButton).toBeInTheDocument();

    fireEvent.click(timestampButton);

    const timestampContent = screen.getByText(/Date should be formatted as/i);

    expect(timestampContent).toBeInTheDocument();
  });

  it('Renders No Override Tab for Dashboard', async () => {
    await asyncRender(<TimeRangeDropdown {...defaultProps} currentTimeRange={{}} noOverride />);

    const noOverrideContent = screen.getByText(/No Date\/Time Override chosen./i);
    const noOverrideButton = screen.getByRole('button', { name: /no override/i });

    expect(noOverrideButton).toBeInTheDocument();
    expect(noOverrideContent).toBeInTheDocument();
  });
});
