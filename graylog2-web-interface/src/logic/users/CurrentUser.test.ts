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

import { alice, bob } from 'fixtures/users';
import type { UserJSON } from 'logic/users/User';
import Session from 'logic/session/Session';

import CurrentUser from './CurrentUser';

const mockGetUser = jest.fn<Promise<UserJSON>, [string]>();

jest.mock('@graylog/server-api', () => ({
  Users: {
    get: (username: string) => mockGetUser(username),
  },
}));

describe('CurrentUser', () => {
  beforeEach(() => {
    Session.setUsername(undefined);
    mockGetUser.mockClear();
  });

  it('loads current user when session starts and clears it when session ends', async () => {
    mockGetUser.mockResolvedValue(alice.toJSON());

    Session.setUsername('alice');

    await waitFor(() => expect(CurrentUser.get()).toEqual(alice.toJSON()));

    Session.setUsername(undefined);

    expect(CurrentUser.get()).toBeUndefined();
  });

  it('does not reload current user if username does not change', async () => {
    mockGetUser.mockResolvedValue(alice.toJSON());

    Session.setUsername('alice');
    Session.setValidating(true);
    Session.setUsername('alice');

    await waitFor(() => expect(CurrentUser.get()).toEqual(alice.toJSON()));

    expect(mockGetUser).toHaveBeenCalledTimes(1);
  });

  it('ignores response for previous session', async () => {
    let resolveAlice: (user: unknown) => void;
    mockGetUser
      .mockReturnValueOnce(
        new Promise((resolve) => {
          resolveAlice = resolve;
        }),
      )
      .mockResolvedValueOnce(bob.toJSON());

    Session.setUsername('alice');
    Session.setUsername('bob');

    await waitFor(() => expect(CurrentUser.get()).toEqual(bob.toJSON()));

    resolveAlice(alice.toJSON());
    await Promise.resolve();

    expect(CurrentUser.get()).toEqual(bob.toJSON());
  });

  it('reloads current user', async () => {
    mockGetUser.mockResolvedValue(alice.toJSON());
    Session.setUsername('alice');
    await waitFor(() => expect(CurrentUser.get()).toEqual(alice.toJSON()));

    await CurrentUser.reload();

    expect(mockGetUser).toHaveBeenCalledTimes(2);
  });
});
