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

import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';

const SystemShutdownStore = Reflux.createStore({
  sourceUrl: (nodeId) => `/cluster/${nodeId}/shutdown`,

  shutdown(nodeId) {
    return fetch('POST', URLUtils.qualifyUrl(this.sourceUrl(nodeId)))
      .then(
        () => {
          this.trigger({});
          UserNotification.success(`Node '${nodeId}' will shutdown shortly`);
        },
        (error) => {
          UserNotification.error(`Shutting down node '${nodeId}' failed: ${error}`,
            `Could not send shutdown signal to node '${nodeId}'`);
        },
      );
  },
});

export default SystemShutdownStore;
