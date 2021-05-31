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
import { fireEvent, render, screen } from 'wrappedTestingLibrary';
import * as React from 'react';
import MockStore from 'helpers/mocking/StoreMock';
import mockSearchClusterConfig from 'fixtures/searchClusterConfig';

import TimeRangeDropdownButton from './TimeRangeDropdownButton';

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

describe('TimeRangeDropdownButton', () => {
  it('button can be clicked and Popover appears', async () => {
    const toggleShow = jest.fn();
    render(<TimeRangeDropdownButton toggleShow={toggleShow} onPresetSelectOpen={() => {}} setCurrentTimeRange={() => {}}><></></TimeRangeDropdownButton>);

    const timeRangeButton = screen.getByLabelText('Open Time Range Selector');

    fireEvent.click(timeRangeButton);

    expect(toggleShow).toHaveBeenCalled();
  });

  it('changes time range when selecting relative time range preset', async () => {
    const setCurrentTimeRange = jest.fn();
    render(<TimeRangeDropdownButton toggleShow={() => {}} onPresetSelectOpen={() => {}} setCurrentTimeRange={setCurrentTimeRange}><></></TimeRangeDropdownButton>);

    const timeRangeButton = screen.getByLabelText('Open time range preset select');
    fireEvent.click(timeRangeButton);
    const rangePresetOption = await screen.findByText('30 minutes');
    fireEvent.click(rangePresetOption);

    expect(setCurrentTimeRange).toHaveBeenCalledWith({
      type: 'relative',
      from: 1800,
    });
  });
});
