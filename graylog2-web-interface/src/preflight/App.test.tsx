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

import fetch from 'logic/rest/FetchProvider';
import { asMock } from 'helpers/mocking';
import useWindowConfirmMock from 'helpers/mocking/useWindowConfirmMock';

import App from './App';

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve({})));

jest.mock('util/AppConfig', () => ({
  gl2ServerUrl: () => 'https://example.org/',
}));

jest.mock('preflight/hooks/useServerAvailability', () =>
  jest.fn(() => ({
    data: false,
  })),
);

jest.mock('preflight/hooks/useDataNodes', () =>
  jest.fn(() => ({
    data: [],
    isFetching: false,
    isInitialLoading: false,
    error: undefined,
  })),
);

jest.mock('preflight/hooks/useDataNodesCA', () =>
  jest.fn(() => ({
    data: undefined,
    isInitialLoading: false,
    isFetching: false,
    error: undefined,
  })),
);

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

describe('App', () => {
  useWindowConfirmMock();

  beforeEach(() => {
    asMock(fetch).mockReturnValue(Promise.resolve());
  });

  it('should render', async () => {
    renderPreflight(<App />);

    await screen.findByText(
      /It looks like you are starting Graylog for the first time and have not configured a data node./,
    );
  });
});
