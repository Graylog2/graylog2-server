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
import Store from 'logic/local-storage/Store';
import { singleton } from 'logic/singleton';
import createExternalStore from 'logic/createExternalStore';

export type SessionState = { username: string | undefined };
export type SessionEvent = 'logout' | 'validated';

const USERNAME_KEY = 'username';

const createSession = () => {
  const store = createExternalStore<SessionState>({ username: undefined });
  const eventListeners: Record<SessionEvent, Set<() => void>> = { logout: new Set(), validated: new Set() };
  const isLoggedIn = () => !!store.getState().username;
  const notify = (event: SessionEvent) => eventListeners[event].forEach((listener) => listener());
  const setUsername = (username: string | undefined) => {
    if (username) {
      Store.set(USERNAME_KEY, username);
    } else {
      Store.delete(USERNAME_KEY);
    }

    store.setState({ username });
  };

  return {
    getState: store.getState,
    subscribe: store.subscribe,
    isLoggedIn,
    storedUsername: (): string | undefined => Store.get(USERNAME_KEY),
    setUsername,
    endSession: () => {
      if (isLoggedIn()) {
        setUsername(undefined);
        notify('logout');
      }
    },
    waitForLogin: () =>
      isLoggedIn()
        ? Promise.resolve()
        : new Promise<void>((resolve) => {
            const unsubscribe = store.subscribe(() => {
              if (isLoggedIn()) {
                unsubscribe();
                resolve();
              }
            });
          }),
    on: (event: SessionEvent, listener: () => void) => {
      eventListeners[event].add(listener);

      return () => {
        eventListeners[event].delete(listener);
      };
    },
    notifyValidated: () => notify('validated'),
  };
};

const Session = singleton('core.Session', createSession);

export default Session;
