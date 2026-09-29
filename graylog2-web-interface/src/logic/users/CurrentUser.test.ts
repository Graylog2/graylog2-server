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

import { Users } from '@graylog/server-api';

import { asMock } from 'helpers/mocking';
import { alice, bob } from 'fixtures/users';
import type User from 'logic/users/User';
import Session from 'logic/session/Session';

import CurrentUser from './CurrentUser';

jest.mock('@graylog/server-api', () => ({
  Users: {
    get: jest.fn(),
  },
}));

const userSummary = (user: User) => user.toJSON() as unknown as Awaited<ReturnType<typeof Users.get>>;

describe('CurrentUser', () => {
  beforeEach(() => {
    Session.setUsername(undefined);
    asMock(Users.get).mockClear();
  });

  it('loads current user when session starts and clears it when session ends', async () => {
    asMock(Users.get).mockResolvedValue(userSummary(alice));

    Session.setUsername('alice');

    await waitFor(() => expect(CurrentUser.get()).toEqual(alice.toJSON()));

    Session.setUsername(undefined);

    expect(CurrentUser.get()).toBeUndefined();
  });

  it('does not reload current user if username does not change', async () => {
    asMock(Users.get).mockResolvedValue(userSummary(alice));

    Session.setUsername('alice');
    Session.setValidating(true);
    Session.setUsername('alice');

    await waitFor(() => expect(CurrentUser.get()).toEqual(alice.toJSON()));

    expect(Users.get).toHaveBeenCalledTimes(1);
  });

  it('ignores response for previous session', async () => {
    let resolveAlice: (user: unknown) => void;
    asMock(Users.get)
      .mockReturnValueOnce(
        new Promise((resolve) => {
          resolveAlice = resolve;
        }),
      )
      .mockResolvedValueOnce(userSummary(bob));

    Session.setUsername('alice');
    Session.setUsername('bob');

    await waitFor(() => expect(CurrentUser.get()).toEqual(bob.toJSON()));

    resolveAlice(userSummary(alice));
    await Promise.resolve();

    expect(CurrentUser.get()).toEqual(bob.toJSON());
  });

  it('reloads current user', async () => {
    asMock(Users.get).mockResolvedValue(userSummary(alice));
    Session.setUsername('alice');
    await waitFor(() => expect(CurrentUser.get()).toEqual(alice.toJSON()));

    await CurrentUser.reload();

    expect(Users.get).toHaveBeenCalledTimes(2);
  });
});
