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
import { SystemSessions } from '@graylog/server-api';

import { qualifyUrl } from 'util/URLUtils';
import { Builder } from 'logic/rest/FetchProvider';
import Session from 'logic/session/Session';

export const login = (username: string, password: string, host: string) =>
  SystemSessions.newSession({ username, password, host }).then((response) => {
    Session.setUsername(response.username);

    return { username: response.username };
  });

// Plain request without error handler, as a 401 for it would otherwise trigger another logout.
const terminateSession = () =>
  new Builder('DELETE', qualifyUrl('/system/sessions/')).build().then(
    (response: Response) => {
      if (!response.ok && response.status !== 401) {
        throw new Error(`Terminating session failed with status ${response.status}.`);
      }

      Session.endSession();
    },
    () => Session.endSession(),
  );

Session.setLogoutHandler(terminateSession);

export const logout = () => Session.logout();

export const validate = () => {
  const storedUsername = Session.storedUsername();

  return SystemSessions.validateSession().then((response) => {
    if (response.is_valid) {
      Session.setUsername(response.username ?? storedUsername);
    } else if (storedUsername) {
      Session.setUsername(undefined);
    }

    Session.notifyValidated();

    return response;
  });
};
