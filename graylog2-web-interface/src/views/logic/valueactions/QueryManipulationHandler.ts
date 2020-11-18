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
import * as Immutable from 'immutable';

import type { Store } from 'stores/StoreTypes';
import { QueriesActions, QueriesStore } from 'views/stores/QueriesStore';
import { ViewStore } from 'views/stores/ViewStore';
import { GlobalOverrideActions, GlobalOverrideStore } from 'views/stores/GlobalOverrideStore';
import SearchActions from 'views/actions/SearchActions';

import Query from '../queries/Query';
import View from '../views/View';
import GlobalOverride from '../search/GlobalOverride';
import type { ElasticsearchQueryString, QueryId } from '../queries/Query';

function connectToStore<State>(store: Store<State>, updateFn: (state: State) => unknown): void {
  updateFn(store.getInitialState());
  store.listen(updateFn);
}

export default class QueryManipulationHandler {
  queries: Immutable.OrderedMap<QueryId, Query>;

  view: View;

  globalOverride: GlobalOverride | undefined | null;

  constructor() {
    connectToStore(QueriesStore, (queries) => { this.queries = queries; });
    connectToStore(ViewStore, ({ view }) => { this.view = view; });
    connectToStore(GlobalOverrideStore, (globalOverride) => { this.globalOverride = globalOverride; });
  }

  _queryStringFromActiveQuery = (queryId: QueryId): string => {
    const query = this.queries.get(queryId);

    return query.query.query_string;
  };

  _queryStringFromGlobalOverride = () => {
    const { query_string: queryString }: ElasticsearchQueryString = this.globalOverride && this.globalOverride.query
      ? this.globalOverride.query
      : { type: 'elasticsearch', query_string: '' };

    return queryString;
  };

  currentQueryString = (queryId: QueryId) => (this.view.type === View.Type.Search
    ? this._queryStringFromActiveQuery(queryId)
    : this._queryStringFromGlobalOverride());

  updateQueryString = (queryId: QueryId, queryString: string) => (this.view.type === View.Type.Search
    ? QueriesActions.query(queryId, queryString)
    : GlobalOverrideActions.query(queryString).then(SearchActions.refresh));
}
