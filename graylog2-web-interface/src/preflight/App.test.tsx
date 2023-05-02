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
import { screen, renderPreflight } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import fetch from 'logic/rest/FetchProvider';

import App from './App';

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));

jest.mock('util/AppConfig', () => ({
  gl2ServerUrl: () => 'https://example.org/',
}));

jest.mock('preflight/hooks/useServerAvailability', () => jest.fn(() => ({
  data: false,
})));

jest.mock('preflight/hooks/useDataNodes', () => jest.fn(() => ({
  data: [],
  isFetching: false,
  isInitialLoading: false,
  error: undefined,
})));

describe('App', () => {
  let windowConfirm;
  let windowLocation;

  beforeAll(() => {
    windowConfirm = window.confirm;
    window.confirm = jest.fn(() => true);

    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { reload: jest.fn() },
    });
  });

  afterAll(() => {
    window.confirm = windowConfirm;
    Object.defineProperty(window, 'location', { configurable: true, value: windowLocation });
  });

  it('should render', async () => {
    renderPreflight(<App />);

    await screen.findByText(/It looks like you are starting Graylog for the first time and have not configured a data node./);
  });

  it('should render loading page after resuming startup', async () => {
    renderPreflight(<App />);

    userEvent.click(await screen.findByRole('button', { name: /resume startup/i }));

    await screen.findByText(/The Graylog server is currently starting./);

    expect(fetch).toHaveBeenCalledWith('POST', expect.stringContaining('/api/status/finish-config'), undefined, false);
  });
});
