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
import { screen, renderPreflight, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import fetch from 'logic/rest/FetchProvider';
import useDataNodes from 'preflight/hooks/useDataNodes';
import { asMock } from 'helpers/mocking';
import UserNotification from 'preflight/util/UserNotification';

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

jest.mock('preflight/util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

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

  it('should resume startup and display loading page', async () => {
    renderPreflight(<App />);

    const resumeStartupButton = await screen.findByRole('button', {
      name: /resume startup/i,
    });

    userEvent.click(resumeStartupButton);

    await waitFor(() => expect(fetch).toHaveBeenCalledWith('POST', expect.stringContaining('/api/status/finish-config'), undefined, false));
    await screen.findByText(/The Graylog server is currently starting./);
  });

  it('should display confirm dialog on resume startup when there is no Graylog data node', async () => {
    asMock(useDataNodes).mockReturnValue({
      data: [],
      isFetching: false,
      isInitialLoading: false,
      error: undefined,
    });

    renderPreflight(<App />);

    const resumeStartupButton = screen.getByRole('button', {
      name: /resume startup/i,
    });

    userEvent.click(resumeStartupButton);

    await waitFor(() => expect(window.confirm).toHaveBeenCalledWith('Are you sure you want to resume startup without a running Graylog data node?'));
    await waitFor(() => expect(fetch).toHaveBeenCalledWith('POST', expect.stringContaining('/api/status/finish-config'), undefined, false));
  });

  it('should display error when resuming startup failed', async () => {
    asMock(fetch).mockImplementation(() => Promise.reject(new Error('Unexpected error!')));

    renderPreflight(<App />);

    const resumeStartupButton = screen.getByRole('button', {
      name: /resume startup/i,
    });

    userEvent.click(resumeStartupButton);

    await waitFor(() => expect(fetch).toHaveBeenCalledWith('POST', expect.stringContaining('/api/status/finish-config'), undefined, false));
    await waitFor(() => expect(UserNotification.error).toHaveBeenCalledWith('Resuming startup failed with error: Error: Unexpected error!', 'Could not resume startup'));
  });
});
