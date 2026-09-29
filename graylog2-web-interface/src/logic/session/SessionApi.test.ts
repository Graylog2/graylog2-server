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

const mockNewSession = jest.fn<Promise<{ username: string }>, [unknown]>();
const mockValidateSession = jest.fn<Promise<{ is_valid: boolean; username?: string }>, []>();
let mockLogoutResponse: Promise<unknown>;

jest.mock('@graylog/server-api', () => ({
  SystemSessions: {
    newSession: (request: unknown) => mockNewSession(request),
    validateSession: () => mockValidateSession(),
  },
}));

jest.mock('logic/rest/FetchProvider', () => ({
  Builder: jest.fn(() => ({ build: () => mockLogoutResponse })),
}));

describe('SessionApi', () => {
  beforeEach(() => {
    Session.setUsername(undefined);
  });

  it('stores username after login', async () => {
    mockNewSession.mockResolvedValue({ username: 'alice' });

    await login('alice', 'secret', 'localhost');

    expect(Session.getState().username).toBe('alice');
  });

  it('ends session and notifies listeners once after logout', async () => {
    Session.setUsername('alice');
    mockLogoutResponse = Promise.resolve({ ok: true, status: 204 });
    const onLogout = jest.fn();
    const unsubscribe = Session.on('logout', onLogout);

    await Promise.all([logout(), logout()]);

    expect(Session.isLoggedIn()).toBe(false);
    expect(onLogout).toHaveBeenCalledTimes(1);

    unsubscribe();
  });

  it('keeps session if logout fails', async () => {
    Session.setUsername('alice');
    mockLogoutResponse = Promise.resolve({ ok: false, status: 500 });
    const onLogout = jest.fn();
    const unsubscribe = Session.on('logout', onLogout);

    await logout();

    expect(Session.isLoggedIn()).toBe(true);
    expect(onLogout).not.toHaveBeenCalled();

    unsubscribe();
  });

  it('logs in after validating a valid session', async () => {
    mockValidateSession.mockResolvedValue({ is_valid: true, username: 'alice' });
    const onValidated = jest.fn(() => expect(Session.getState().username).toBe('alice'));
    const unsubscribe = Session.on('validated', onValidated);

    await validate();

    expect(onValidated).toHaveBeenCalledTimes(1);

    unsubscribe();
  });

  it('removes stored session if it is not valid anymore', async () => {
    Session.setUsername('alice');
    mockValidateSession.mockResolvedValue({ is_valid: false });

    await validate();

    expect(Session.isLoggedIn()).toBe(false);
    expect(Session.storedUsername()).toBeUndefined();
  });
});
