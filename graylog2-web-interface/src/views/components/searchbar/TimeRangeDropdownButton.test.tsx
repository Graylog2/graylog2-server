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
import { fireEvent, render, screen, waitFor } from 'wrappedTestingLibrary';
import { Formik } from 'formik';
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

const selectRangePreset = async (optionLabel: string) => {
  const timeRangeButton = screen.getByLabelText('Open time range preset select');
  fireEvent.click(timeRangeButton);
  const rangePresetOption = await screen.findByText(optionLabel);
  fireEvent.click(rangePresetOption);
};

describe('TimeRangeDropdownButton', () => {
  type SUTProps = Partial<React.ComponentProps<typeof TimeRangeDropdownButton>> & {
    onSubmit?: () => void
  }

  const SUTTimeRangeDropDownButton = ({ onSubmit = () => {}, ...props }: SUTProps) => (
    <Formik initialValues={{ selectedFields: [] }} onSubmit={onSubmit}>
      <TimeRangeDropdownButton toggleShow={() => {}} onPresetSelectOpen={() => {}} setCurrentTimeRange={() => {}} {...props}><></></TimeRangeDropdownButton>
    </Formik>
  );

  it('button can be clicked and Popover appears', async () => {
    const toggleShow = jest.fn();
    render(<SUTTimeRangeDropDownButton toggleShow={toggleShow}><></></SUTTimeRangeDropDownButton>);

    const timeRangeButton = screen.getByLabelText('Open Time Range Selector');

    fireEvent.click(timeRangeButton);

    expect(toggleShow).toHaveBeenCalled();
  });

  it('changes time range and submits form when selecting relative time range preset', async () => {
    const setCurrentTimeRange = jest.fn();
    const onSubmit = jest.fn();
    render(<SUTTimeRangeDropDownButton setCurrentTimeRange={setCurrentTimeRange} onSubmit={onSubmit}><></></SUTTimeRangeDropDownButton>);

    await selectRangePreset('30 minutes');

    expect(setCurrentTimeRange).toHaveBeenCalledWith({
      type: 'relative',
      from: 1800,
    });

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
  });

  it('updates time range with range attribute when selecting "all messages" relative time range preset', async () => {
    const setCurrentTimeRange = jest.fn();
    const onSubmit = jest.fn();
    render(<SUTTimeRangeDropDownButton setCurrentTimeRange={setCurrentTimeRange} onSubmit={onSubmit}><></></SUTTimeRangeDropDownButton>);

    await selectRangePreset('all messages');

    expect(setCurrentTimeRange).toHaveBeenCalledWith({
      type: 'relative',
      range: 0,
    });

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
  });
});
