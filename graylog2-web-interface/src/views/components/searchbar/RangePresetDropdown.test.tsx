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

import MockStore from 'helpers/mocking/StoreMock';
import mockSearchClusterConfig from 'fixtures/searchClusterConfig';

import RangePresetDropdown from './RangePresetDropdown';

jest.mock('stores/configurations/ConfigurationsStore', () => ({
  ConfigurationsStore: MockStore(),
}));

jest.mock('views/stores/SearchConfigStore', () => ({
  SearchConfigActions: {
    refresh: jest.fn(() => Promise.resolve()),
  },
  SearchConfigStore: {
    listen: () => jest.fn(),
    getInitialState: () => ({ searchesClusterConfig: mockSearchClusterConfig }),
  },
}));

describe('RangePresetDropdown', () => {
  it('should not call onChange prop when selecting "Configure Ranges" option.', async () => {
    const onSelectOption = jest.fn();
    render(<RangePresetDropdown onChange={onSelectOption} />);

    const timeRangeButton = screen.getByLabelText('Open time range preset select');
    fireEvent.click(timeRangeButton);
    const rangePresetOption = await screen.findByText('Configure Ranges');
    fireEvent.click(rangePresetOption);

    expect(onSelectOption).not.toHaveBeenCalled();
  });
});
