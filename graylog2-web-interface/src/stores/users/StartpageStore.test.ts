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
import fetch from 'logic/rest/FetchProvider';
import asMock from 'helpers/mocking/AsMock';
import UserNotification from 'util/UserNotification';

import { StartpageStore } from './StartpageStore';

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));
jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

describe('StartpageStore', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should send the current timezone together with the startpage', async () => {
    const listener = jest.fn();
    const unsubscribe = StartpageStore.listen(listener);

    await StartpageStore.set({ id: 'user-id', timezone: 'Europe/Berlin' }, 'stream', 'stream-id');

    unsubscribe();

    expect(fetch).toHaveBeenCalledWith('PUT', expect.stringContaining('/users/user-id'), {
      timezone: 'Europe/Berlin',
      startpage: { type: 'stream', id: 'stream-id' },
    });
    expect(listener).toHaveBeenCalled();
    expect(UserNotification.success).toHaveBeenCalled();
  });

  it('should show an error when the update fails', async () => {
    asMock(fetch).mockImplementationOnce(() => Promise.reject(new Error('Boom')));
    const listener = jest.fn();
    const unsubscribe = StartpageStore.listen(listener);

    await StartpageStore.set({ id: 'user-id', timezone: 'Europe/Berlin' }, 'stream', 'stream-id');

    unsubscribe();

    expect(listener).not.toHaveBeenCalled();
    expect(UserNotification.error).toHaveBeenCalled();
  });
});
