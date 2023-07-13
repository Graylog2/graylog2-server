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
import { fireEvent, render, screen } from 'wrappedTestingLibrary';

import mockSearchClusterConfig from 'fixtures/searchClusterConfig';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import asMock from 'helpers/mocking/AsMock';

import RangePresetDropdown from './TimeRangePresetDropdown';

jest.mock('hooks/useSearchConfiguration', () => jest.fn());

describe('RangePresetDropdown', () => {
  it('should not call onChange prop when selecting "Configure Ranges" option.', async () => {
    asMock(useSearchConfiguration).mockReturnValue({
      config: mockSearchClusterConfig,
      refresh: jest.fn(),
    });

    const onSelectOption = jest.fn();
    render(<RangePresetDropdown onChange={onSelectOption} availableOptions={[]} />);

    const timeRangePresetButton = screen.getByLabelText('Open time range preset select');
    fireEvent.click(timeRangePresetButton);
    const rangePresetOption = await screen.findByText('Configure Ranges');
    fireEvent.click(rangePresetOption);

    expect(onSelectOption).not.toHaveBeenCalled();
  });

  it('filtrate options by limit', async () => {
    asMock(useSearchConfiguration).mockReturnValue({
      config: {
        ...mockSearchClusterConfig,
        query_time_range_limit: 'PT6M',
      },
      refresh: jest.fn(),
    });

    const onSelectOption = jest.fn();

    render(<RangePresetDropdown onChange={onSelectOption}
                                availableOptions={[
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
                                ]} />);

    const timeRangePresetButton = screen.getByLabelText('Open time range preset select');
    fireEvent.click(timeRangePresetButton);

    const tenMinTR = screen.queryByText('Keyword ten min');
    await screen.findByText('5 minutes');

    expect(tenMinTR).not.toBeInTheDocument();
  });
});
