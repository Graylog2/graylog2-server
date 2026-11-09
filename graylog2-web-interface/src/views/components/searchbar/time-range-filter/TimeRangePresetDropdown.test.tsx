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
jest.mock('util/AppConfig', () => ({
  ...jest.requireActual('util/AppConfig').default,
  gl2AppPathPrefix: () => '/graylog',
}));

describe('TimeRangePresetDropdown', () => {
  const openTimeRangePresetSelect = async () => {
    await userEvent.click(
      await screen.findByRole('button', {
        name: /open time range preset select/i,
      }),
    );

    await screen.findByRole('menu');
  };

  it('should prefix "Configure presets" link with app path prefix', async () => {
    asMock(useSearchConfiguration).mockReturnValue({
      config: mockSearchClusterConfig,
      refresh: jest.fn(),
      isInitialLoading: false,
    });

    render(<TimeRangePresetDropdown onChange={jest.fn()} limitDuration={0} />);

    await openTimeRangePresetSelect();

    expect(await screen.findByRole('menuitem', { name: /configure presets/i })).toHaveAttribute(
      'href',
      '/graylog/system/configurations',
    );
  });

  it('should not call onChange prop when selecting "Configure Ranges" option.', async () => {
    asMock(useSearchConfiguration).mockReturnValue({
      config: mockSearchClusterConfig,
      refresh: jest.fn(),
      isInitialLoading: false,
    });

    const onSelectOption = jest.fn();
    render(<TimeRangePresetDropdown onChange={onSelectOption} limitDuration={0} />);

    await openTimeRangePresetSelect();
    await screen.findByRole('menuitem', { name: /15 minutes/i });

    const rangePresetOption = screen.getByRole('menuitem', {
      name: /configure presets/i,
    });
    await userEvent.click(rangePresetOption);

    await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());

    expect(onSelectOption).not.toHaveBeenCalled();

    await waitFor(() => {
      expect(screen.queryByRole('menuitem', { name: /configure preset/i })).not.toBeInTheDocument();
    });
  });

  it('filtrate options by limit', async () => {
    const limitDuration = 36000; // 6 Minutes
    asMock(useSearchConfiguration).mockReturnValue({
      config: {
        ...mockSearchClusterConfig,
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
      isInitialLoading: false,
    });

    const onSelectOption = jest.fn();

    render(<TimeRangePresetDropdown onChange={onSelectOption} limitDuration={limitDuration} />); //

    await openTimeRangePresetSelect();

    await screen.findByRole('menu');
    await screen.findByRole('menuitem', { name: /5 minutes/i });

    expect(screen.queryByText('Keyword ten min')).not.toBeInTheDocument();
  });

  it('keeps options older than limit when their range is within limit', async () => {
    asMock(useSearchConfiguration).mockReturnValue({
      config: {
        ...mockSearchClusterConfig,
        quick_access_timerange_presets: [
          {
            id: '639843f5-049a-4532-8a54-102da850b7f1',
            timerange: { from: 7 * 86400, to: 6 * 86400, type: 'relative' },
            description: 'Day a week ago',
          },
          {
            id: '8dda08e9-cd23-44ff-b4eb-edeb7a704cf4',
            timerange: { from: '2018-11-14 01:00:00.000', to: '2018-11-14 13:00:00.000', type: 'absolute' },
            description: 'Half a day in 2018',
          },
          {
            id: '0b9cbd77-3e55-4b6b-8a4e-6f7c2b1fd0a2',
            timerange: { from: '2018-11-12 13:00:00.000', to: '2018-11-14 13:00:00.000', type: 'absolute' },
            description: 'Two days in 2018',
          },
        ],
      },
      refresh: jest.fn(),
      isInitialLoading: false,
    });

    render(<TimeRangePresetDropdown onChange={jest.fn()} limitDuration={86400} />);

    await openTimeRangePresetSelect();

    await screen.findByRole('menuitem', { name: /day a week ago/i });
    await screen.findByRole('menuitem', { name: /half a day in 2018/i });

    expect(screen.queryByText('Two days in 2018')).not.toBeInTheDocument();
  });
});
