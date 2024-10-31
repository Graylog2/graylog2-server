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
import { render, screen } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import useHotkey from 'hooks/useHotkey';

import KeyCapture from './KeyCapture';

jest.mock('hooks/useHotkey', () => jest.fn());

describe('<KeyCapture />', () => {
  const mockUseHotkey = jest.fn();

  beforeEach(() => {
    asMock(useHotkey).mockImplementation(mockUseHotkey);
  });

  it('renders its children', () => {
    render(<KeyCapture shortcuts={[{ scope: 'general', callback: () => {}, actionKey: 'test' }]}>The children</KeyCapture>);

    expect(screen.getByText('The children')).toBeInTheDocument();
  });

  it('runs useHotkey hooks with proper params', () => {
    const mockCallback = jest.fn();

    render(<KeyCapture shortcuts={[
      { scope: 'general', callback: mockCallback, actionKey: 'make' },
      { scope: 'search', callback: mockCallback, actionKey: 'do' },
    ]} />);

    expect(mockUseHotkey).toHaveBeenCalledWith({ scope: 'general', callback: mockCallback, actionKey: 'make' });
    expect(mockUseHotkey).toHaveBeenCalledWith({ scope: 'search', callback: mockCallback, actionKey: 'do' });
  });
});
