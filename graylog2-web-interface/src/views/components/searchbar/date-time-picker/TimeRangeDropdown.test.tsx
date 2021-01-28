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
import { NoTimeRangeOverride } from 'src/views/logic/queries/Query';
import { StoreMock as MockStore } from 'helpers/mocking';
import { act } from 'react-dom/test-utils';
import { SearchBarFormValues } from 'src/views/Constants';

import ToolsStore from 'stores/tools/ToolsStore';

import { DateTimeContext } from './DateTimeProvider';
import OriginalTimeRangeDropDown from './TimeRangeDropdown';

const mockSearchClusterConfig = {
  query_time_range_limit: 'P3D',
  relative_timerange_options: {
    PT10M: 'Search in the last 5 minutes',
    PT15M: 'Search in the last 15 minutes',
    PT30M: 'Search in the last 30 minutes',
    PT1H: 'Search in the last 1 hour',
    PT2H: 'Search in the last 2 hours',
    PT8H: 'Search in the last 8 hours',
    P1D: 'Search in the last 1 day',
    P2D: 'Search in the last 2 days',
    P5D: 'Search in the last 5 days',
    P7D: 'Search in the last 7 days',
    P14D: 'Search in the last 14 days',
    P30D: 'Search in the last 30 days',
    PT0S: 'Search in all messages',
    P45D: '45 last days',
  },
  surrounding_timerange_options: {
    PT1S: '1 second',
    PT5S: '5 seconds',
    PT10S: '10 seconds',
    PT30S: '30 seconds',
    PT1M: '1 minute',
    PT5M: '5 minutes',
    PT3M: '3 minutes',
  },
  surrounding_filter_fields: [
    'file',
    'source',
    'gl2_source_input',
    'source_file',
  ],
  analysis_disabled_fields: [
    'full_message',
    'message',
  ],
};

jest.mock('views/stores/SearchConfigStore', () => ({
  SearchConfigActions: {
    refresh: jest.fn(() => Promise.resolve()),
  },
  SearchConfigStore: MockStore(
    ['listen', () => jest.fn()],
    'get',
    ['searchesClusterConfig', () => mockSearchClusterConfig],
    ['getInitialState', () => mockSearchClusterConfig],
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
} as const;

type Props = {
  noOverride?: boolean,
  currentTimeRange: SearchBarFormValues['timerange'] | NoTimeRangeOverride,
  setCurrentTimeRange: (nextTimeRange: SearchBarFormValues['timerange'] | NoTimeRangeOverride) => void,
  toggleDropdownShow: () => void,
};

const TimeRangeDropdown = (allProps: Props) => (
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
      range: 300,
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
