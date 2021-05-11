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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import { StoreMock as MockStore } from 'helpers/mocking';
import userEvent from '@testing-library/user-event';
import mockSearchClusterConfig from 'fixtures/searchClusterConfig';

import { DateTimeContext } from './DateTimeProvider';
import OriginalTimeRangeDropDown, { TimeRangeDropdownProps } from './TimeRangeDropdown';

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
  position: 'bottom',
} as const;

const TimeRangeDropdown = (allProps: TimeRangeDropdownProps) => (
  <DateTimeContext.Provider value={{
    limitDuration: 259200,
  }}>
    <OriginalTimeRangeDropDown {...allProps} />
  </DateTimeContext.Provider>
);

describe('TimeRangeDropdown relative time range', () => {
  it('Display warning when emptying from range value input', async () => {
    render(<TimeRangeDropdown {...defaultProps} />);

    const fromRangeValueInput = screen.getByTitle('Set the from value');
    const submitButton = screen.getByRole('button', {
      name: /apply/i,
    });

    userEvent.type(fromRangeValueInput, '{backspace}');

    await screen.findByText('Cannot be empty.');

    expect(submitButton).toBeDisabled();
  });

  it('Display warning when emptying to range value input', async () => {
    const props = {
      ...defaultProps,
      currentTimeRange: {
        type: 'relative',
        from: 300,
        to: 240,
      },
    };
    render(<TimeRangeDropdown {...props} />);

    const toRangeValueInput = screen.getByTitle('Set the to value');
    const submitButton = screen.getByRole('button', {
      name: /apply/i,
    });

    userEvent.type(toRangeValueInput, '{backspace}');

    await screen.findByText('Cannot be empty.');

    expect(submitButton).toBeDisabled();
  });

  it('allow emptying from and to ranges and typing in completely new values', async () => {
    const setCurrentTimeRangeStub = jest.fn();
    const props = {
      ...defaultProps,
      currentTimeRange: {
        type: 'relative',
        from: 300,
        to: 240,
      },
      setCurrentTimeRange: setCurrentTimeRangeStub,
    };
    render(<TimeRangeDropdown {...props} />);

    const fromRangeValueInput = screen.getByTitle('Set the from value');
    const toRangeValueInput = screen.getByTitle('Set the to value');
    const submitButton = screen.getByRole('button', {
      name: /apply/i,
    });

    userEvent.type(fromRangeValueInput, '{backspace}7');
    userEvent.type(toRangeValueInput, '{backspace}6');
    userEvent.click(submitButton);

    await waitFor(() => expect(setCurrentTimeRangeStub).toHaveBeenCalledTimes(1));

    expect(setCurrentTimeRangeStub).toHaveBeenCalledWith({
      type: 'relative',
      from: 420,
      to: 360,
    });
  });
});
