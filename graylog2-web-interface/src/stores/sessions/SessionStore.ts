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
import Reflux from 'reflux';

import Store from 'logic/local-storage/Store';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import { Builder } from 'logic/rest/FetchProvider';
import { singletonStore, singletonActions } from 'logic/singleton';

type SessionActionsType = {
  login: (username: string, password: string, host: string) => Promise<unknown>,
  logout: () => Promise<unknown>,
  validate: () => Promise<unknown>,
}
export const SessionActions = singletonActions(
  'core.Session',
  () => Reflux.createActions<SessionActionsType>({
    login: { asyncResult: true },
    logout: { asyncResult: true },
    validate: { asyncResult: true },
  }),
);

export type SessionStoreState = { username: string, validatingSession: boolean };

export const SessionStore = singletonStore(
  'core.Session',
  () => Reflux.createStore<SessionStoreState>({
    listenables: [SessionActions],
    sourceUrl: '/system/sessions',
    username: undefined,
    validatingSession: false,

    init() {
      this.validate();
    },
    getInitialState() {
      return this.getSessionInfo();
    },

    login(username: string, password: string, host: string) {
      const builder = new Builder('POST', qualifyUrl(this.sourceUrl))
        .json({ username, password, host });
      const promise = builder.build()
        .then((response) => {
          return { username: response?.username };
        });

      SessionActions.login.promise(promise);
    },
    logout() {
      const promise = new Builder('DELETE', qualifyUrl(`${this.sourceUrl}/`))
        .build()
        .then((resp) => {
          if (resp.ok || resp.status === 401) {
            this._removeSession();
          }
        }, this._removeSession);

      SessionActions.logout.promise(promise);
    },

    validate() {
      const username = Store.get('username');

      this.validatingSession = true;
      this._propagateState();
      const promise = this._validateSession()
        .then((response) => {
          if (response.is_valid) {
            return SessionActions.login.completed({
              username: response.username ?? username,
            });
          }

          if (username) {
            this._removeSession();
          }

          return response;
        })
        .finally(() => {
          this.validatingSession = false;
          this._propagateState();
        });

      SessionActions.validate.promise(promise);
    },
    _validateSession() {
      return new Builder('GET', qualifyUrl(ApiRoutes.SessionsApiController.validate().url))
        .json()
        .build();
    },

    _removeSession() {
      Store.delete('username');
      this.username = undefined;
      this._propagateState();
    },

    _propagateState() {
      this.trigger(this.getSessionInfo());
    },

    loginCompleted(sessionInfo) {
      const { username } = sessionInfo;
      Store.set('username', username);
      this.username = username;
      this._propagateState();
    },
    isLoggedIn() {
      return !!this.username;
    },
    getSessionInfo() {
      return { username: this.username, validatingSession: this.validatingSession };
    },
  }),
);
