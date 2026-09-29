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

const SESSIONS_URL = '/system/sessions';

export const login = (username: string, password: string, host: string) =>
  new Builder('POST', qualifyUrl(SESSIONS_URL))
    .json({ username, password, host })
    .build()
    .then((response: { username?: string }) => {
      Session.setUsername(response?.username);

      return { username: response?.username };
    });

export const logout = () =>
  new Builder('DELETE', qualifyUrl(`${SESSIONS_URL}/`))
    .build()
    .then(
      (response: Response) => {
        if (response.ok || response.status === 401) {
          Session.setUsername(undefined);
        }
      },
      () => Session.setUsername(undefined),
    )
    .then(() => Session.notify('logout'));

export const validate = () => {
  const storedUsername = Session.storedUsername();
  Session.setValidating(true);

  return SystemSessions.validateSession()
    .then((response) => {
      if (response.is_valid) {
        Session.setUsername(response.username ?? storedUsername);
      } else if (storedUsername) {
        Session.setUsername(undefined);
      }

      return response;
    })
    .finally(() => Session.setValidating(false))
    .then((response) => {
      Session.notify('validated');

      return response;
    });
};
