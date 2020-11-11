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

const StartpageStore = Reflux.createStore({
  listenables: [],

  set(userId, type, id) {
    const url = qualifyUrl(ApiRoutes.UsersApiController.update(userId).url);
    const payload = {};

    if (type && id) {
      payload.type = type;
      payload.id = id;
    }

    return fetch('PUT', url, { startpage: payload })
      .then(
        (response) => {
          this.trigger();
          UserNotification.success('Your start page was changed successfully');

          return response;
        },
        (error) => UserNotification.error(`Changing your start page failed with error: ${error}`, 'Could not change your start page'),
      );
  },
});

export default StartpageStore;
