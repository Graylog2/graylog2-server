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
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';

const PluginsStore = Reflux.createStore({
  sourceUrl: (nodeId) => `/cluster/${nodeId}/plugins`,

  list(nodeId) {
    const promise = fetch('GET', URLUtils.qualifyUrl(this.sourceUrl(nodeId)))
      .then(
        (response) => response.plugins,
        (error) => UserNotification.error(`Getting plugins on node "${nodeId}" failed: ${error}`, 'Could not get plugins'),
      );

    return promise;
  },
});

export default PluginsStore;
