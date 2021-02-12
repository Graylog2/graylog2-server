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

import * as URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import ApiRoutes from 'routing/ApiRoutes';
import ActionsProvider from 'injection/ActionsProvider';

const AlarmCallbackHistoryActions = ActionsProvider.getActions('AlarmCallbackHistory');

const AlarmCallbackHistoryStore = Reflux.createStore({
  listenables: [AlarmCallbackHistoryActions],
  histories: undefined,

  getInitialState() {
    return { histories: this.histories };
  },

  list(streamId, alertId) {
    const url = URLUtils.qualifyUrl(ApiRoutes.AlarmCallbackHistoryApiController.list(streamId, alertId).url);
    const promise = fetch('GET', url)
      .then(
        (response) => {
          this.histories = response.histories;
          this.trigger({ histories: this.histories });

          return this.histories;
        },
        (error) => {
          UserNotification.error(`Fetching notification history for alert '${alertId}' failed with status: ${error}`,
            'Could not retrieve notification history.');
        },
      );

    AlarmCallbackHistoryActions.list.promise(promise);
  },
});

export default AlarmCallbackHistoryStore;
