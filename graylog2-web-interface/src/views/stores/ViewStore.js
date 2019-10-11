// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';
import { get, isEqualWith } from 'lodash';

import ViewGenerator from 'views/logic/views/ViewGenerator';
import SearchTypesGenerator from 'views/logic/searchtypes/SearchTypesGenerator';
import { QueriesActions } from 'views/actions/QueriesActions';
import type { Properties, ViewType } from 'views/logic/views/View';
import View from 'views/logic/views/View';
import type { QuerySet } from 'views/logic/search/Search';
import Search from 'views/logic/search/Search';
import ViewState from 'views/logic/views/ViewState';
import Query from 'views/logic/queries/Query';
import SearchActions from 'views/actions/SearchActions';
import { singletonActions, singletonStore } from 'views/logic/singleton';
import { ViewManagementActions } from './ViewManagementStore';
import type { RefluxActions } from './StoreTypes';

export type ViewStoreState = {
  activeQuery: string,
  view: View,
  dirty: boolean,
};

type ViewActionsType = RefluxActions<{
  create: (ViewType) => Promise<ViewStoreState>,
  description: (string) => Promise<ViewStoreState>,
  load: (View) => Promise<ViewStoreState>,
  properties: (Properties) => Promise<void>,
  search: (Search) => Promise<View>,
  selectQuery: (string) => Promise<string>,
  state: (ViewState) => Promise<View>,
  summary: (string) => Promise<void>,
  title: (string) => Promise<void>,
}>;

export const ViewActions: ViewActionsType = singletonActions(
  'views.View',
  () => Reflux.createActions({
    create: { asyncResult: true },
    dashboardState: { asyncResult: true },
    description: { asyncResult: true },
    load: { asyncResult: true },
    properties: { asyncResult: true },
    search: { asyncResult: true },
    selectQuery: { asyncResult: true },
    state: { asyncResult: true },
    summary: { asyncResult: true },
    title: { asyncResult: true },
  }),
);

type ViewStoreUnsubscribe = () => void;

type ViewStoreType = {
  listen: ((ViewStoreState) => void) => ViewStoreUnsubscribe,
  getInitialState: () => ViewStoreState,
};

export const ViewStore: ViewStoreType = singletonStore(
  'views.View',
  () => Reflux.createStore({
    listenables: [ViewActions],
    view: undefined,
    activeQuery: undefined,
    dirty: false,

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

    create(type: ViewType) {
      const [view] = this._updateSearch(ViewGenerator(type));
      this.view = view;
      const queries: QuerySet = get(view, 'search.queries', Immutable.Set());
      this.activeQuery = queries.first().id;

      const promise = ViewActions.search(view.search)
        .then(() => {
          this.dirty = false;
        })
        .then(() => this._trigger());

      ViewActions.create.promise(promise.then(() => this._state()));

      return promise;
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

      const promise = (isModified ? ViewActions.search(view.search) : Promise.resolve(view)).then(() => this._trigger());

      QueriesActions.create.promise(promise);
    },
    description(newDescription: string) {
      this.dirty = true;
      this.view = this.view.toBuilder().description(newDescription).build();
      this._trigger();
      const promise = Promise.resolve(this._state());
      ViewActions.description.promise(promise);
      return promise;
    },
    load(view: View): Promise<ViewStoreState> {
      this.view = view;
      this.dirty = false;

      /* Select selected query (activeQuery) or first query in view (for now).
         Selected query might become a property on the view later. */
      const queries = get(view, 'search.queries', Immutable.List());
      const firstQueryId = get(queries.first(), 'id');
      const selectedQuery = this.activeQuery && queries.find(q => (q.id === this.activeQuery)) ? this.activeQuery : firstQueryId;
      this.selectQuery(selectedQuery);

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
    summary(newSummary) {
      this.dirty = true;
      this.view = this.view.toBuilder().summary(newSummary).build();
      this._trigger();
    },
    title(newTitle) {
      this.dirty = true;
      this.view = this.view.toBuilder().title(newTitle).build();
      this._trigger();
    },
    _updateSearch(view: View): [View, boolean] {
      if (!view.search) {
        return [view, false];
      }
      const oldWidgets = get(this.view, 'state') && this.view.state.map(s => s.widgets);
      const newWidgets = get(view, 'state') && view.state.map(s => s.widgets);
      if (!isEqualWith(oldWidgets, newWidgets, Immutable.is)) {
        const states = view.state;
        const searchTypes = states.map(state => SearchTypesGenerator(state.widgets));

        const search = get(view, 'search');
        const newQueries = search.queries.map(q => q.toBuilder().searchTypes(searchTypes.get(q.id, {}).searchTypes).build());
        const newSearch = search.toBuilder().queries(newQueries).build();
        let newView = view.toBuilder().search(newSearch).build();

        searchTypes.map(({ widgetMapping }) => widgetMapping)
          .forEach((widgetMapping, queryId) => {
            const newStates = newView.state;
            if (states.has(queryId)) {
              newView = newView.toBuilder().state(newStates.update(queryId, state => state.toBuilder().widgetMapping(widgetMapping).build())).build();
            }
          });
        return [newView, true];
      }

      return [view, false];
    },
    _state(): ViewStoreState {
      return {
        activeQuery: this.activeQuery,
        view: this.view,
        dirty: this.dirty,
      };
    },
    _trigger() {
      this.trigger(this._state());
    },
  }),
);
