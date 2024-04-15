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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import { Formik } from 'formik';
import * as React from 'react';

import MockStore from 'helpers/mocking/StoreMock';
import mockSearchClusterConfig from 'fixtures/searchClusterConfig';

import TimeRangeFilterButtons from './TimeRangeFilterButtons';

jest.mock('stores/configurations/ConfigurationsStore', () => ({
  ConfigurationsStore: MockStore(),
}));

jest.mock('hooks/useSearchConfiguration', () => () => ({
  config: { ...mockSearchClusterConfig, query_time_range_limit: undefined },
  refresh: () => {},
}));

const selectRangePreset = async (optionLabel: string) => {
  const timeRangePresetButton = screen.getByRole('button', {
    name: /open time range preset select/i,
  });
  userEvent.click(timeRangePresetButton);
  const rangePresetOption = await screen.findByRole('menuitem', {
    name: new RegExp(optionLabel, 'i'),
  });
  userEvent.click(rangePresetOption);
};

describe('TimeRangeFilterButtons', () => {
  type SUTProps = Partial<React.ComponentProps<typeof TimeRangeFilterButtons>> & {
    onSubmit?: () => void
  }

  const SUTTimeRangeFilterButtons = ({ onSubmit = () => {}, ...props }: SUTProps) => (
    <Formik initialValues={{ selectedFields: [] }} onSubmit={onSubmit}>
      <TimeRangeFilterButtons toggleShow={() => {}} onPresetSelectOpen={() => {}} setCurrentTimeRange={() => {}} {...props} />
    </Formik>
  );

  it('button can be clicked and Popover appears', async () => {
    const toggleShow = jest.fn();
    render(<SUTTimeRangeFilterButtons toggleShow={toggleShow} />);

    const timeRangePickerButton = screen.getByLabelText('Open Time Range Selector');

    userEvent.click(timeRangePickerButton);

    expect(toggleShow).toHaveBeenCalled();
  });

  it('changes time range and submits form when selecting relative time range preset', async () => {
    const setCurrentTimeRange = jest.fn();
    const onSubmit = jest.fn();
    render(<SUTTimeRangeFilterButtons setCurrentTimeRange={setCurrentTimeRange} onSubmit={onSubmit} />);

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
    render(<SUTTimeRangeFilterButtons setCurrentTimeRange={setCurrentTimeRange} onSubmit={onSubmit} />);

    await selectRangePreset('all messages');

    expect(setCurrentTimeRange).toHaveBeenCalledWith({
      type: 'relative',
      range: 0,
    });

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
  });
});
