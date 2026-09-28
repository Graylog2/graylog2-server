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

import { asMock } from 'helpers/mocking';
import { alice, bob } from 'fixtures/users';
import fetch from 'logic/rest/FetchProvider';
import Session from 'logic/session/Session';

import CurrentUser from './CurrentUser';

jest.mock('logic/rest/FetchProvider', () => jest.fn());

describe('CurrentUser', () => {
  beforeEach(() => {
    Session.setUsername(undefined);
    asMock(fetch).mockClear();
  });

  it('loads current user when session starts and clears it when session ends', async () => {
    asMock(fetch).mockResolvedValue(alice.toJSON());

    Session.setUsername('alice');

    await waitFor(() => expect(CurrentUser.get()).toEqual(alice.toJSON()));

    Session.setUsername(undefined);

    expect(CurrentUser.get()).toBeUndefined();
  });

  it('does not reload current user if username does not change', async () => {
    asMock(fetch).mockResolvedValue(alice.toJSON());

    Session.setUsername('alice');
    Session.setValidating(true);
    Session.setUsername('alice');

    await waitFor(() => expect(CurrentUser.get()).toEqual(alice.toJSON()));

    expect(fetch).toHaveBeenCalledTimes(1);
  });

  it('ignores response for previous session', async () => {
    let resolveAlice: (user: unknown) => void;
    asMock(fetch)
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
    asMock(fetch).mockResolvedValue(alice.toJSON());
    Session.setUsername('alice');
    await waitFor(() => expect(CurrentUser.get()).toEqual(alice.toJSON()));

    await CurrentUser.reload();

    expect(fetch).toHaveBeenCalledTimes(2);
  });
});
