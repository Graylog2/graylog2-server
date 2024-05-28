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
import userEvent from '@testing-library/user-event';

import OriginalTimeRangePicker from './TimeRangePicker';

jest.mock('stores/tools/ToolsStore', () => ({}));
jest.mock('hooks/useHotkey', () => jest.fn());

describe('TimeRangePicker relative time range', () => {
  const defaultProps = {
    show: true,
    currentTimeRange: {
      type: 'relative',
      from: 300,
    },
    noOverride: false,
    setCurrentTimeRange: jest.fn(),
    toggleDropdownShow: jest.fn(),
    position: 'bottom',
    limitDuration: 259200,
  } as const;

  const TimeRangePicker = (allProps: Partial<React.ComponentProps<typeof TimeRangePicker>>) => (
    <OriginalTimeRangePicker {...defaultProps} {...allProps}>
      <button type="button">Open</button>
    </OriginalTimeRangePicker>
  );

  const getSubmitButton = () => screen.getByRole('button', { name: /Update time range/i });

  it('display warning when emptying from range value input', async () => {
    render(<TimeRangePicker />);

    const fromRangeValueInput = screen.getByTitle('Set the from value');

    const submitButton = getSubmitButton();
    userEvent.type(fromRangeValueInput, '{backspace}');

    await screen.findByText('Cannot be empty.');

    expect(submitButton).toBeDisabled();
  });

  it('display warning when emptying to range value input', async () => {
    const currentTimeRange = {
      type: 'relative',
      from: 300,
      to: 240,
    };
    render(<TimeRangePicker currentTimeRange={currentTimeRange} />);

    const toRangeValueInput = screen.getByTitle('Set the to value');
    const submitButton = getSubmitButton();

    userEvent.type(toRangeValueInput, '{backspace}');

    await screen.findByText('Cannot be empty.');

    expect(submitButton).toBeDisabled();
  });

  it('allow emptying from and to ranges and typing in completely new values', async () => {
    const setCurrentTimeRangeStub = jest.fn();
    const currentTimeRange = {
      type: 'relative',
      from: 300,
      to: 240,
    };
    render(<TimeRangePicker currentTimeRange={currentTimeRange} setCurrentTimeRange={setCurrentTimeRangeStub} />);

    const fromRangeValueInput = await screen.findByTitle('Set the from value');
    const toRangeValueInput = screen.getByTitle('Set the to value');
    const submitButton = getSubmitButton();

    await userEvent.type(fromRangeValueInput, '{backspace}7');
    await userEvent.type(toRangeValueInput, '{backspace}6');

    await waitFor(() => expect(submitButton).not.toBeDisabled());
    await userEvent.click(submitButton);

    await waitFor(() => expect(setCurrentTimeRangeStub).toHaveBeenCalledTimes(1));

    expect(setCurrentTimeRangeStub).toHaveBeenCalledWith({
      type: 'relative',
      from: 420,
      to: 360,
    });
  });
});
