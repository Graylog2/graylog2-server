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
import { render, screen } from 'wrappedTestingLibrary';
import * as React from 'react';
import userEvent from '@testing-library/user-event';

import { NO_TIMERANGE_OVERRIDE } from 'views/Constants';

import TimeRangeDisplay from './TimeRangeDisplay';

describe('TimeRangeDisplay', () => {
  it('opens the date time range picker on click', async () => {
    const toggleShow = jest.fn();
    render(<TimeRangeDisplay toggleDropdownShow={toggleShow} timerange={{ type: 'relative', from: 300 }} />);

    const timeRangeDisplay = screen.getByRole('button', {
      name: 'Search Time Range, Opens Time Range Selector On Click',
    });

    await userEvent.click(timeRangeDisplay);

    expect(toggleShow).toHaveBeenCalled();
  });

  it('renders from and to values when centerTimestamps is enabled', () => {
    render(
      <TimeRangeDisplay
        centerTimestamps
        timerange={{ type: 'absolute', from: '2026-01-01 00:00:00', to: '2026-01-02 00:00:00' }}
      />,
    );

    expect(screen.getByTestId('from')).toHaveTextContent('From: 2026-01-01 00:00:00');
    expect(screen.getByTestId('to')).toHaveTextContent('Until: 2026-01-02 00:00:00');
  });

  it('renders no override text when centerTimestamps is enabled', () => {
    render(<TimeRangeDisplay centerTimestamps timerange={NO_TIMERANGE_OVERRIDE} />);

    expect(screen.getByText('No Override')).toBeInTheDocument();
  });

  it('does not crash when timerange is undefined', () => {
    render(<TimeRangeDisplay timerange={undefined} />);

    expect(screen.getByTestId('from')).toBeInTheDocument();
    expect(screen.getByTestId('to')).toBeInTheDocument();
  });
});
