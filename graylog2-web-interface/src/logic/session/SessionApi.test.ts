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
import Session from 'logic/session/Session';

import { login, logout, validate } from './SessionApi';

let mockResponse: unknown;

jest.mock('logic/rest/FetchProvider', () => ({
  Builder: jest.fn(() => {
    const builder = {
      json: () => builder,
      setHeaders: () => builder,
      build: () => Promise.resolve(mockResponse),
    };

    return builder;
  }),
}));

const respondWith = (response: unknown) => {
  mockResponse = response;
};

describe('SessionApi', () => {
  beforeEach(() => {
    Session.setUsername(undefined);
  });

  it('stores username after login', async () => {
    respondWith({ username: 'alice' });

    await login('alice', 'secret', 'localhost');

    expect(Session.getState().username).toBe('alice');
  });

  it('clears session and notifies listeners after logout', async () => {
    Session.setUsername('alice');
    respondWith({ ok: true, status: 204 });
    const onLogout = jest.fn();
    const unsubscribe = Session.on('logout', onLogout);

    await logout();

    expect(Session.isLoggedIn()).toBe(false);
    expect(onLogout).toHaveBeenCalledTimes(1);

    unsubscribe();
  });

  it('logs in after validating a valid session', async () => {
    respondWith({ is_valid: true, username: 'alice' });
    const onValidated = jest.fn(() =>
      expect(Session.getState()).toEqual({ username: 'alice', validatingSession: false }),
    );
    const unsubscribe = Session.on('validated', onValidated);

    const result = validate();

    expect(Session.getState().validatingSession).toBe(true);

    await result;

    expect(onValidated).toHaveBeenCalledTimes(1);

    unsubscribe();
  });

  it('removes stored session if it is not valid anymore', async () => {
    Session.setUsername('alice');
    respondWith({ is_valid: false });

    await validate();

    expect(Session.isLoggedIn()).toBe(false);
    expect(Session.storedUsername()).toBeUndefined();
  });
});
