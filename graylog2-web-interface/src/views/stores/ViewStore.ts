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
import * as Immutable from 'immutable';
import { get } from 'lodash';

import type { RefluxActions, Store } from 'stores/StoreTypes';
import UpdateSearchForWidgets from 'views/logic/views/UpdateSearchForWidgets';
import ViewGenerator from 'views/logic/views/ViewGenerator';
import { QueriesActions } from 'views/actions/QueriesActions';
import type { Properties, ViewType, ViewStateMap } from 'views/logic/views/View';
import type View from 'views/logic/views/View';
import type { QuerySet } from 'views/logic/search/Search';
import type Search from 'views/logic/search/Search';
import type ViewState from 'views/logic/views/ViewState';
import type Query from 'views/logic/queries/Query';
import SearchActions from 'views/actions/SearchActions';
import { singletonActions, singletonStore } from 'logic/singleton';
import type { QueryId, TimeRange, ElasticsearchQueryString } from 'views/logic/queries/Query';

import { ViewManagementActions } from './ViewManagementStore';
import isEqualForSearch from './isEqualForSearch';

export type ViewStoreState = {
  activeQuery: QueryId,
  view: View,
  dirty: boolean,
  isNew: boolean,
};

type ViewActionsType = RefluxActions<{
  create: (
    type: ViewType,
    streamId?: string,
    timeRange?: TimeRange,
    queryString?: ElasticsearchQueryString,
  ) => Promise<ViewStoreState>,
  load: (view: View, isNew?: boolean, queryId?: string) => Promise<ViewStoreState>,
  properties: (properties: Properties) => Promise<void>,
  search: (search: Search) => Promise<View>,
  selectQuery: (queryId: string) => Promise<string>,
  state: (state: ViewStateMap) => Promise<View>,
  update: (view: View) => Promise<ViewStoreState>,
}>;

export const ViewActions: ViewActionsType = singletonActions(
  'views.View',
  () => Reflux.createActions({
    create: { asyncResult: true },
    dashboardState: { asyncResult: true },
    load: { asyncResult: true },
    properties: { asyncResult: true },
    search: { asyncResult: true },
    selectQuery: { asyncResult: true },
    state: { asyncResult: true },
    setNew: { asyncResult: true },
    update: { asyncResult: true },
  }),
);

type ViewStoreType = Store<ViewStoreState>;

const _selectedQuery = (queries: QuerySet = Immutable.Set(), activeQuery: string, queryId: string): QueryId => {
  const selectedQuery = queryId ?? activeQuery;

  if (selectedQuery && queries.find(({ id }) => (id === selectedQuery))) {
    return selectedQuery;
  }

  return queries.first()?.id;
};

export const ViewStore: ViewStoreType = singletonStore(
  'views.View',
  () => Reflux.createStore({
    listenables: [ViewActions],
    view: undefined,
    activeQuery: undefined,
    dirty: false,
    isNew: false,

    init() {
      QueriesActions.create.listen(this.createQuery);

      ViewManagementActions.update.completed.listen(() => {
        this.dirty = false;
        this._trigger();
      });
    },

    getInitialState(): ViewStoreState {
      return this._state();
    },

    create(
      type: ViewType,
      streamId: string = null,
      timeRange?: TimeRange,
      queryString?: ElasticsearchQueryString,
    ) {
      return ViewGenerator(type, streamId, timeRange, queryString)
        .then((newView) => {
          const [view] = this._updateSearch(newView);

          this.view = view;
          const queries: QuerySet = get(view, 'search.queries', Immutable.Set());

          this.activeQuery = queries.first().id;

          return view;
        }).then((view) => {
          const promise = ViewActions.search(view.search)
            .then(() => {
              this.dirty = false;
              this.isNew = true;
            })
            .then(() => this._trigger());

          ViewActions.create.promise(promise.then(() => this._state()));

          return promise;
        });
    },
    createQuery(query: Query, viewState: ViewState) {
      if (query.id === undefined) {
        throw new Error('Unable to add query without id to view.');
      }

      const { search }: View = this.view;

      const newQueries = search.queries.add(query);

      const newSearch = search.toBuilder().queries(newQueries).build();
      const newState = this.view.state.set(query.id, viewState);

      this.dirty = true;
      const [view, isModified] = this._updateSearch(this.view.toBuilder()
        .state(newState)
        .search(newSearch)
        .build());

      this.view = view;
      this.activeQuery = query.id;

      const promise = (isModified ? ViewActions.search(view.search) : Promise.resolve(view)).then(
        () => {
          this._trigger();

          return Immutable.OrderedMap<QueryId, Query>(newSearch.queries.map((q) => [q.id, q]).toArray());
        },
      );

      QueriesActions.create.promise(promise);
    },
    update(view: View) {
      this.dirty = false;
      this.view = view;
      this._trigger();
      const promise = Promise.resolve(this._state());

      ViewActions.update.promise(promise);

      return promise;
    },
    load(view: View, isNew = false, queryId: string = undefined): Promise<ViewStoreState> {
      this.view = view;
      this.dirty = false;

      /* Select query id passed through URL, selected query (activeQuery) or first query in view (for now).
         Selected query might become a property on the view later. */
      const queries = view?.search?.queries;
      const selectedQuery = _selectedQuery(queries, this.activeQuery, queryId);

      this.selectQuery(selectedQuery);
      this.isNew = isNew;

      const promise = Promise.resolve(this._state());

      ViewActions.load.promise(promise);

      return promise;
    },
    properties(newProperties: Properties) {
      this.dirty = true;
      this.view = this.view.toBuilder().properties(newProperties).build();
      this._trigger();
    },
    search(newSearch: Search) {
      const promise = SearchActions.create(newSearch).then(({ search }) => {
        this.dirty = true;
        this.view = this.view.toBuilder().search(search).build();
        this._trigger();

        return this.view;
      });

      ViewActions.search.promise(promise);

      return promise;
    },
    selectQuery(queryId) {
      this.activeQuery = queryId;
      this._trigger();
      const promise = Promise.resolve(this.view);

      ViewActions.selectQuery.promise(promise);

      return promise;
    },
    state(newState: ViewState) {
      this.dirty = true;
      const [view, isModified] = this._updateSearch(this.view.toBuilder().state(newState).build());

      this.view = view;
      const promise = (isModified ? ViewActions.search(view.search) : Promise.resolve(view)).then(() => this._trigger());

      ViewActions.state.promise(promise);

      return promise;
    },
    _updateSearch(view: View): [View, boolean] {
      if (!view.search) {
        return [view, false];
      }

      const oldWidgets = get(this.view, 'state') && this.view.state.map((s) => s.widgets);
      const newWidgets = get(view, 'state') && view.state.map((s) => s.widgets);

      if (!isEqualForSearch(oldWidgets, newWidgets)) {
        const newView = UpdateSearchForWidgets(view);

        return [newView, true];
      }

      return [view, false];
    },
    _state(): ViewStoreState {
      return {
        activeQuery: this.activeQuery,
        view: this.view,
        dirty: this.dirty,
        isNew: this.isNew,
      };
    },
    _trigger() {
      this.trigger(this._state());
    },
  }),
);
