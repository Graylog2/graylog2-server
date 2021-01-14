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

import AbsoluteTimeInput from './AbsoluteTimeInput';

const defaultProps = {
  dateTime: '1955-05-11 06:15:00.000',
  range: 'from',
  onChange: jest.fn(),
} as const;

describe('AbsoluteTimeInput', () => {
  it('renders', () => {
    render(<AbsoluteTimeInput {...defaultProps} />);

    expect(screen).not.toBeNull();
  });

  it('toggles bod & eod', () => {
    render(<AbsoluteTimeInput {...defaultProps} />);

    const toggleBtn = screen.getByRole('button', { name: /toggle between beginning and end of day/i });

    fireEvent.click(toggleBtn);

    expect(defaultProps.onChange).toHaveBeenCalled();
    expect(defaultProps.onChange).toHaveBeenCalledWith('1955-05-11 00:00:00.000');

    fireEvent.click(toggleBtn);

    expect(defaultProps.onChange).toHaveBeenCalledWith('1955-05-11 23:59:59.999');
  });

  it('reset non-numeric inputs to 0', () => {
    render(<AbsoluteTimeInput {...defaultProps} />);

    const inputHour = screen.getByRole('spinbutton', { name: /from hour/i });

    fireEvent.change(inputHour, { target: { value: '!' } });

    expect(defaultProps.onChange).toHaveBeenCalled();
    expect(defaultProps.onChange).toHaveBeenCalledWith('1955-05-11 00:15:00.000');
  });

  it('allows numeric input', () => {
    render(<AbsoluteTimeInput {...defaultProps} />);

    const inputHour = screen.getByRole('spinbutton', { name: /from hour/i });

    fireEvent.change(inputHour, { target: { value: '10' } });

    expect(defaultProps.onChange).toHaveBeenCalled();
    expect(defaultProps.onChange).toHaveBeenCalledWith('1955-05-11 10:15:00.000');
  });

  it('does not allow numbers over their maximum', () => {
    render(<AbsoluteTimeInput {...defaultProps} />);

    const inputHour = screen.getByRole('spinbutton', { name: /from hour/i });
    const inputMinute = screen.getByRole('spinbutton', { name: /from minutes/i });
    const inputSeconds = screen.getByRole('spinbutton', { name: /from seconds/i });
    const inputMS = screen.getByRole('spinbutton', { name: /from milliseconds/i });

    fireEvent.change(inputHour, { target: { value: '99' } });

    expect(defaultProps.onChange).toHaveBeenCalledWith('1955-05-11 23:15:00.000');

    fireEvent.change(inputMinute, { target: { value: '999' } });

    expect(defaultProps.onChange).toHaveBeenCalledWith('1955-05-11 06:59:00.000');

    fireEvent.change(inputSeconds, { target: { value: '999' } });

    expect(defaultProps.onChange).toHaveBeenCalledWith('1955-05-11 06:15:59.000');

    fireEvent.change(inputMS, { target: { value: '9999' } });

    expect(defaultProps.onChange).toHaveBeenCalledWith('1955-05-11 06:15:00.999');
  });

  it('does not try to parse an empty date', () => {
    render(<AbsoluteTimeInput {...defaultProps} />);

    const inputHour = screen.getByRole('spinbutton', { name: /from hour/i });

    fireEvent.change(inputHour, { target: { value: '' } });

    expect(defaultProps.onChange).toHaveBeenCalled();
    expect(defaultProps.onChange).toHaveBeenCalledWith('1955-05-11 00:15:00.000');
  });
});
