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
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import AbsoluteTimeInput from './AbsoluteTimeInput';

const defaultProps = {
  dateTime: '1955-05-11 06:15:00',
  range: 'from',
  onChange: jest.fn(),
} as const;

describe('AbsoluteTimeInput', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders', () => {
    render(<AbsoluteTimeInput {...defaultProps} />);

    expect(screen).not.toBeNull();
  });

  it('toggles bod & eod', async () => {
    render(<AbsoluteTimeInput {...defaultProps} />);

    const toggleBtn = screen.getByRole('button', { name: /toggle between beginning and end of day/i });

    await userEvent.click(toggleBtn);

    expect(defaultProps.onChange).toHaveBeenCalled();
    expect(defaultProps.onChange).toHaveBeenCalledWith('1955-05-11 00:00:00');

    await userEvent.click(toggleBtn);

    expect(defaultProps.onChange).toHaveBeenCalledWith('1955-05-11 23:59:59');
  });

  it('reset non-numeric inputs to 0', async () => {
    render(<AbsoluteTimeInput {...defaultProps} />);

    const inputHour = screen.getByRole('spinbutton', { name: /from hour/i });

    await userEvent.clear(inputHour);
    await userEvent.type(inputHour, '!');

    expect(defaultProps.onChange).toHaveBeenCalled();
    expect(defaultProps.onChange).toHaveBeenCalledWith('1955-05-11 00:15:00');
  });

  it('allows numeric input', async () => {
    render(<AbsoluteTimeInput {...defaultProps} />);

    const inputHour = screen.getByRole('spinbutton', { name: /from hour/i });

    await userEvent.clear(inputHour);
    await userEvent.type(inputHour, '10');

    expect(defaultProps.onChange).toHaveBeenCalled();
    expect(defaultProps.onChange).toHaveBeenCalledWith('1955-05-11 10:15:00');
  });

  it('does not allow numbers over their maximum', async () => {
    render(<AbsoluteTimeInput {...defaultProps} />);

    const inputHour = screen.getByRole('spinbutton', { name: /from hour/i });
    const inputMinute = screen.getByRole('spinbutton', { name: /from minutes/i });
    const inputSeconds = screen.getByRole('spinbutton', { name: /from seconds/i });

    await userEvent.clear(inputHour);
    await userEvent.type(inputHour, '99');

    expect(defaultProps.onChange).toHaveBeenCalledWith('1955-05-11 23:15:00');

    await userEvent.clear(inputMinute);
    await userEvent.type(inputMinute, '999');

    expect(defaultProps.onChange).toHaveBeenCalledWith('1955-05-11 06:59:00');

    await userEvent.clear(inputSeconds);
    await userEvent.type(inputSeconds, '999');

    expect(defaultProps.onChange).toHaveBeenCalledWith('1955-05-11 06:15:59');
  });

  it('does not try to parse an empty date', async () => {
    render(<AbsoluteTimeInput {...defaultProps} />);

    const inputHour = screen.getByRole('spinbutton', { name: /from hour/i });

    await userEvent.clear(inputHour);

    expect(defaultProps.onChange).toHaveBeenCalled();
    expect(defaultProps.onChange).toHaveBeenCalledWith('1955-05-11 00:15:00');
  });
});
