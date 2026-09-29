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
import { Users } from '@graylog/server-api';

import type { UserJSON } from 'logic/users/User';
import { singleton } from 'logic/singleton';
import createExternalStore from 'logic/createExternalStore';
import Session from 'logic/session/Session';

export type CurrentUserState = { currentUser: UserJSON | undefined };

const createCurrentUser = () => {
  const store = createExternalStore<CurrentUserState>({ currentUser: undefined });

  const load = (username: string): Promise<UserJSON> =>
    Users.get(encodeURIComponent(username)).then((user) => {
      const currentUser = user as UserJSON;

      if (Session.getState().username === username) {
        store.setState({ currentUser });
      }

      return currentUser;
    });

  let sessionUsername: string | undefined;
  const onSessionChange = () => {
    const { username } = Session.getState();

    if (username === sessionUsername) {
      return;
    }

    sessionUsername = username;

    if (username) {
      load(username).catch(() => {
        if (sessionUsername === username) {
          sessionUsername = undefined;
        }
      });
    } else {
      store.setState({ currentUser: undefined });
    }
  };

  Session.subscribe(onSessionChange);
  onSessionChange();

  return {
    getState: store.getState,
    subscribe: store.subscribe,
    reload: (): Promise<UserJSON | void> => {
      const { username } = Session.getState();

      return username ? load(username) : Promise.resolve();
    },
  };
};

const CurrentUser = singleton('core.CurrentUser', createCurrentUser);

export default CurrentUser;
