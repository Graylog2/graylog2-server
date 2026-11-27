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

import ApiRoutes from 'routing/ApiRoutes';
import UserNotification from 'util/UserNotification';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import { singletonStore } from 'logic/singleton';
import type User from 'logic/users/User';

export const StartpageStore = singletonStore('core.Startpage', () =>
  Reflux.createStore({
    listenables: [],

    set(user: Pick<User, 'id' | 'timezone'>, type: string, id: string) {
      const url = qualifyUrl(ApiRoutes.UsersApiController.update(encodeURIComponent(user.id)).url);
      const payload: { type?: string; id?: string } = {};

      if (type && id) {
        payload.type = type;
        payload.id = id;
      }

      // The backend treats a missing timezone as null and clears it, so we always send the current value.
      return fetch('PUT', url, { timezone: user.timezone, startpage: payload }).then(
        (response: unknown) => {
          this.trigger();
          UserNotification.success('Your start page was changed successfully');

          return response;
        },
        (error: unknown) =>
          UserNotification.error(
            `Changing your start page failed with error: ${error}`,
            'Could not change your start page',
          ),
      );
    },
  }),
);
