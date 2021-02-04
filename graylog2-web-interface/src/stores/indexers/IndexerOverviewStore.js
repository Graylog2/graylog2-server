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
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import ActionsProvider from 'injection/ActionsProvider';

const IndexerOverviewActions = ActionsProvider.getActions('IndexerOverview');

const IndexerOverviewStore = Reflux.createStore({
  listenables: [IndexerOverviewActions],
  list(indexSetId) {
    const url = URLUtils.qualifyUrl(ApiRoutes.IndexerOverviewApiResource.list(indexSetId).url);
    const promise = fetch('GET', url);

    promise.then(
      (response) => {
        this.trigger({ indexerOverview: response, indexerOverviewError: undefined });
      },
      (error) => {
        if (error.additional && error.additional.status === 503) {
          const errorMessage = (error.additional.body && error.additional.body.message
            ? error.additional.body.message
            : 'Elasticsearch is unavailable. Check your configuration and logs for more information.');

          this.trigger({ indexerOverviewError: errorMessage });
        }
      },
    );

    IndexerOverviewActions.list.promise(promise);

    return promise;
  },
});

export default IndexerOverviewStore;
