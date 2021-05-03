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
// @flow strict
import Reflux from 'reflux';

import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import ActionsProvider from 'injection/ActionsProvider';

const IndexerOverviewActions = ActionsProvider.getActions('IndexerOverview');

export type IndexSummary = {
  size: {
    events: number,
    deleted: number,
    bytes: number,
  },
  range: {
    index_name: string,
    begin: string,
    end: string,
    calculated_at: string,
    took_ms: number,
  },
  is_deflector: boolean,
  is_closed: boolean,
  is_reopened: boolean,
};

export type IndexerOverview = {
  deflector: {
    current_target: string,
    is_up: boolean,
  },
  indexer_cluster: {
    health: {
      status: string,
      name: string,
      shards: {
        active: number,
        initializing: number,
        relocating: number,
        unassigned: number,
      },
    },
  },
  counts: {
    [key: string]: number,
  },
  indices: {
    [key: string]: IndexSummary,
  },
};

const IndexerOverviewStore = Reflux.createStore({
  listenables: [IndexerOverviewActions],
  indexerOverview: undefined,
  indexerOverviewError: undefined,

  getInitialState() {
    return {
      indexerOverview: this.indexerOverview,
      indexerOverviewError: this.indexerOverviewError,
    };
  },

  list(indexSetId: string) {
    const url = qualifyUrl(ApiRoutes.IndexerOverviewApiResource.list(indexSetId).url);
    const promise = fetch('GET', url);

    promise.then(
      (response: IndexerOverview) => {
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
