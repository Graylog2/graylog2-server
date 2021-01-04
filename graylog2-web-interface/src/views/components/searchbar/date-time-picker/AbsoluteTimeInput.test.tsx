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
  onChange: undefined,
} as const;

describe('AbsoluteTimeInput', () => {
  it('renders', () => {
    render(<AbsoluteTimeInput {...defaultProps} />);

    expect(screen).not.toBeNull();
  });

  it('toggles bod & eod', () => {
    const onChange = jest.fn();
    render(<AbsoluteTimeInput {...defaultProps} onChange={onChange} />);

    const toggleBtn = screen.getByRole('button', { name: /toggle between beginning and end of day/i });

    fireEvent.click(toggleBtn);

    expect(onChange).toHaveReturnedWith('1955-05-11 00:00:00.000');

    // fireEvent.click(toggleBtn);
    //
    // expect(defaultProps.onChange).toHaveReturnedWith('1955-05-11 23:59:59.999');
  });

  it('does not allow non-numeric characters', () => {
    render(<AbsoluteTimeInput {...defaultProps} />);

    const inputHour = screen.getByRole('spinbutton', { name: /from hour/i });

    fireEvent.change(inputHour, { target: { value: '/w!' } });

    expect(inputHour).toHaveValue(0);
  });

  it('does allow proper value', () => {
    render(<AbsoluteTimeInput {...defaultProps} />);

    const inputHour = screen.getByRole('spinbutton', { name: /from hour/i });

    fireEvent.change(inputHour, { target: { value: '10' } });

    expect(inputHour).toHaveValue(10);
  });

  it('does not allow numbers over their maximum', () => {
    render(<AbsoluteTimeInput {...defaultProps} />);

    const inputHour = screen.getByRole('spinbutton', { name: /from hour/i });

    fireEvent.change(inputHour, { target: { value: '50' } });

    expect(inputHour).toHaveValue(23);
  });

  it('does not try to parse an empty date', () => {
    render(<AbsoluteTimeInput {...defaultProps} />);

    const inputHour = screen.getByRole('spinbutton', { name: /from hour/i });

    fireEvent.change(inputHour, { target: { value: '' } });

    expect(inputHour).toHaveValue(0);
  });
});
