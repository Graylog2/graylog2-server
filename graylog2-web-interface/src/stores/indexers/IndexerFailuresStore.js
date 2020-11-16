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
import moment from 'moment';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

const IndexerFailuresStore = Reflux.createStore({
  listenables: [],

  list(limit, offset) {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexerFailuresApiController.list(limit, offset).url);

    return fetch('GET', url);
  },

  count(since) {
    const momentSince = since.format ? since : moment(since);
    const isoSince = momentSince.format('YYYY-MM-DDTHH:mm:ss.SSS');
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexerFailuresApiController.count(isoSince).url);

    return fetch('GET', url);
  },
});

export default IndexerFailuresStore;
