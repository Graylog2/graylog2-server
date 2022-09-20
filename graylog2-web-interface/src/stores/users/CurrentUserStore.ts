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

import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import type { UserJSON } from 'logic/users/User';
import { singletonStore } from 'logic/singleton';
import { PreferencesActions } from 'stores/users/PreferencesStore';
import { SessionActions, SessionStore } from 'stores/sessions/SessionStore';
import { StartpageStore } from 'stores/users/StartpageStore';

export type CurrentUserStoreState = {
  currentUser: UserJSON,
};

export const CurrentUserStore = singletonStore(
  'core.CurrentUser',
  () => Reflux.createStore<CurrentUserStoreState>({
    listenables: [SessionActions],
    currentUser: undefined,

    init() {
      this.listenTo(SessionStore, this.sessionUpdate, this.sessionUpdate);
      this.listenTo(StartpageStore, this.reload, this.reload);
      PreferencesActions.saveUserPreferences.completed.listen(this.reload);
    },

    getInitialState() {
      return { currentUser: this.currentUser };
    },

    get() {
      return this.currentUser;
    },

    sessionUpdate(sessionInfo) {
      if (sessionInfo.username) {
        const { username } = sessionInfo;

        this.update(username);
      } else {
        this.currentUser = undefined;
        this.trigger({ currentUser: this.currentUser });
      }
    },

    reload() {
      if (this.currentUser !== undefined) {
        return this.update(this.currentUser.username);
      }

      return Promise.resolve();
    },

    update(username) {
      return fetch('GET', qualifyUrl(ApiRoutes.UsersApiController.loadByUsername(encodeURIComponent(username)).url))
        .then((resp) => {
          this.currentUser = resp;
          this.trigger({ currentUser: this.currentUser });

          return resp;
        });
    },
  }),
);
