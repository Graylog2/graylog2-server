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
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import type { UserJSON } from 'logic/users/User';
import { singleton } from 'logic/singleton';
import createExternalStore from 'logic/createExternalStore';
import Session from 'logic/session/Session';

export type CurrentUserState = { currentUser: UserJSON | undefined };

const createCurrentUser = () => {
  const store = createExternalStore<CurrentUserState>({ currentUser: undefined });

  const load = (username: string): Promise<UserJSON> =>
    fetch<UserJSON>(
      'GET',
      qualifyUrl(ApiRoutes.UsersApiController.loadByUsername(encodeURIComponent(username)).url),
    ).then((currentUser) => {
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
      load(username);
    } else {
      store.setState({ currentUser: undefined });
    }
  };

  Session.subscribe(onSessionChange);
  onSessionChange();

  return {
    getState: store.getState,
    subscribe: store.subscribe,
    get: () => store.getState().currentUser,
    reload: (): Promise<UserJSON | void> => {
      const { currentUser } = store.getState();

      return currentUser ? load(currentUser.username) : Promise.resolve();
    },
  };
};

const CurrentUser = singleton('core.CurrentUser', createCurrentUser);

export default CurrentUser;
