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
import { render, screen, fireEvent } from 'wrappedTestingLibrary';

import ErrorAlert from './ErrorAlert';

describe('ErrorAlert', () => {
  it('should display an Error', () => {
    render(<ErrorAlert>Franz</ErrorAlert>);

    expect(screen.queryByText('Franz')).not.toBeNull();
    expect(screen.queryByText('Runtime Error')).toBeNull();
  });

  it('should display an Runtime Error', () => {
    render(<ErrorAlert runtimeError>Franz</ErrorAlert>);

    expect(screen.queryByText('Franz')).not.toBeNull();
    expect(screen.queryByText('Runtime Error')).not.toBeNull();
  });

  it('should display nothing without children', () => {
    render(<ErrorAlert />);

    expect(screen.queryByText('Franz')).toBeNull();
    expect(screen.queryByText('Runtime Error')).toBeNull();
  });

  it('should call onClose handler', () => {
    const onClose = jest.fn();
    render(<ErrorAlert onClose={onClose}>Franz</ErrorAlert>);

    const closeBtn = screen.getByRole('button');

    fireEvent.click(closeBtn);

    expect(onClose).toHaveBeenCalled();
  });
});
