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
import CurrentUser from 'logic/users/CurrentUser';
import UserNotification from 'util/UserNotification';

import { setStartpage } from './startpage';

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));
jest.mock('logic/users/CurrentUser', () => ({ reload: jest.fn() }));
jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

describe('setStartpage', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should send the current timezone together with the startpage', async () => {
    await setStartpage({ id: 'user-id', timezone: 'Europe/Berlin' }, 'stream', 'stream-id');

    expect(fetch).toHaveBeenCalledWith('PUT', expect.stringContaining('/users/user-id'), {
      timezone: 'Europe/Berlin',
      startpage: { type: 'stream', id: 'stream-id' },
    });
    expect(CurrentUser.reload).toHaveBeenCalled();
    expect(UserNotification.success).toHaveBeenCalled();
  });

  it('should show an error when the update fails', async () => {
    asMock(fetch).mockImplementationOnce(() => Promise.reject(new Error('Boom')));

    await setStartpage({ id: 'user-id', timezone: 'Europe/Berlin' }, 'stream', 'stream-id');

    expect(CurrentUser.reload).not.toHaveBeenCalled();
    expect(UserNotification.error).toHaveBeenCalled();
  });
});
