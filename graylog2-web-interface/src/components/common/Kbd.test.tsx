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
import { render, screen } from 'wrappedTestingLibrary';

import Kbd from './Kbd';

const mockUserAgent = (userAgent: string) =>
  jest.spyOn(window.navigator, 'userAgent', 'get').mockReturnValue(userAgent);

describe('Kbd', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it.each([
    ['mod', '⌘'],
    ['alt', '⌥'],
    ['ctrl', '⌃'],
    ['meta', '⌘'],
  ])('renders %s as %s on macOS', async (name, label) => {
    mockUserAgent('Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5)');

    render(<Kbd name={name} />);

    await screen.findByText(label);
  });

  it.each([
    ['mod', 'Ctrl'],
    ['alt', 'Alt'],
    ['ctrl', 'Ctrl'],
    ['meta', 'Win'],
  ])('renders %s as %s on other platforms', async (name, label) => {
    mockUserAgent('Mozilla/5.0 (X11; Linux x86_64)');

    render(<Kbd name={name} />);

    await screen.findByText(label);
  });

  it('renders other keys unchanged', async () => {
    render(<Kbd name="Enter" />);

    await screen.findByText('Enter');
  });
});
