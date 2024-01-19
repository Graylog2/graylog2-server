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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import mockSearchClusterConfig from 'fixtures/searchClusterConfig';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import asMock from 'helpers/mocking/AsMock';

import TimeRangePresetDropdown from './TimeRangePresetDropdown';

jest.mock('hooks/useSearchConfiguration', () => jest.fn());

describe('TimeRangePresetDropdown', () => {
  const openTimeRangePresetSelect = async () => {
    userEvent.click(await screen.findByRole('button', {
      name: /open time range preset select/i,
    }));

    await screen.findByRole('menu');
  };

  it('should not call onChange prop when selecting "Configure Ranges" option.', async () => {
    asMock(useSearchConfiguration).mockReturnValue({
      config: mockSearchClusterConfig,
      refresh: jest.fn(),
    });

    const onSelectOption = jest.fn();
    render(<TimeRangePresetDropdown onChange={onSelectOption} />);

    await openTimeRangePresetSelect();
    await screen.findByRole('menuitem', { name: /15 minutes/i });

    const rangePresetOption = screen.getByRole('menuitem', {
      name: /configure presets/i,
    });
    userEvent.click(rangePresetOption);

    await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());

    expect(onSelectOption).not.toHaveBeenCalled();

    await waitFor(() => {
      expect(screen.queryByRole('menuitem', { name: /configure preset/i })).not.toBeInTheDocument();
    });
  });

  it('filtrate options by limit', async () => {
    asMock(useSearchConfiguration).mockReturnValue({
      config: {
        ...mockSearchClusterConfig,
        query_time_range_limit: 'PT6M',
        quick_access_timerange_presets: [
          {
            id: '639843f5-049a-4532-8a54-102da850b7f1',
            timerange: {
              from: 300,
              type: 'relative',
            },
            description: '5 minutes',
          },
          {
            id: '8dda08e9-cd23-44ff-b4eb-edeb7a704cf4',
            timerange: {
              keyword: 'Last ten minutes',
              timezone: 'Europe/Berlin',
              type: 'keyword',
            },
            description: 'Keyword ten min',
          },
        ],
      },
      refresh: jest.fn(),
    });

    const onSelectOption = jest.fn();

    render(<TimeRangePresetDropdown onChange={onSelectOption} />);

    await openTimeRangePresetSelect();

    await screen.findByRole('menu');
    await screen.findByRole('menuitem', { name: /5 minutes/i });

    expect(screen.queryByText('Keyword ten min')).not.toBeInTheDocument();
  });
});
