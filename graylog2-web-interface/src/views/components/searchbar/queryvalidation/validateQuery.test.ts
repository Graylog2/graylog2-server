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
import { waitFor } from 'wrappedTestingLibrary';

import fetch from 'logic/rest/FetchProvider';
import { StoreMock as MockStore, asMock } from 'helpers/mocking';
import UserNotification from 'util/UserNotification';

import type { ValidationQuery } from './validateQuery';
import validateQuery from './validateQuery';

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));
jest.mock('stores/users/CurrentUserStore', () => ({ CurrentUserStore: MockStore('get') }));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
}));

describe('validateQuery', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  const validationInput: ValidationQuery = {
    queryString: 'source:',
    timeRange: { type: 'relative', from: 300 } as const,
    streams: ['stream-id'],
  };

  const requestPayload = {
    query: 'source:',
    filter: undefined,
    timerange: { type: 'relative', from: 300 },
    streams: ['stream-id'],
  };

  const userTimezone = 'Europe/Berlin';

  it('calls validate API', async () => {
    await validateQuery(validationInput, userTimezone);

    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));

    expect(fetch).toHaveBeenCalledWith('POST', expect.any(String), requestPayload);
  });

  it('normalizes absolute time ranges', async () => {
    await validateQuery({
      ...validationInput,
      timeRange: { type: 'absolute', from: '2021-01-01 16:00:00.000', to: '2021-01-01 17:00:00.000' },
    }, userTimezone);

    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));

    const expectedPayload = {
      ...requestPayload,
      timerange: {
        type: 'absolute',
        from: '2021-01-01T15:00:00.000+00:00',
        to: '2021-01-01T16:00:00.000+00:00',
      },
    };

    expect(fetch).toHaveBeenCalledWith('POST', expect.any(String), expectedPayload);
  });

  it('should display user notification and return status OK on server error', async () => {
    asMock(fetch).mockImplementation(() => Promise.reject(new Error('Unexpected error')));

    const result = await validateQuery({
      ...validationInput,
      timeRange: { type: 'absolute', from: '2021-01-01 16:00:00.000', to: '2021-01-01 17:00:00.000' },
    }, userTimezone);

    expect(UserNotification.error).toHaveBeenCalledWith('Validating search query failed with status: Error: Unexpected error');
    expect(result).toEqual({ status: 'OK' });
  });
});
