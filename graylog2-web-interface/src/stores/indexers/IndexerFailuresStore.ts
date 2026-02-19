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

import * as URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import { singletonStore } from 'logic/singleton';

export const IndexerFailuresStore = singletonStore('core.IndexerFailures', () =>
  Reflux.createStore({
    listenables: [],

    list(limit: number, offset: number): Promise<unknown> {
      const url = URLUtils.qualifyUrl(ApiRoutes.IndexerFailuresApiController.list(limit, offset).url);

      return fetch('GET', url);
    },

    count(since: unknown): Promise<unknown> {
      const momentSince = (since as { format?: unknown }).format ? (since as moment.Moment) : moment(since as string);
      const isoSince = momentSince.format('YYYY-MM-DDTHH:mm:ss.SSS');
      const url = URLUtils.qualifyUrl(ApiRoutes.IndexerFailuresApiController.count(isoSince as unknown as number).url);

      return fetch('GET', url);
    },
  }),
);
