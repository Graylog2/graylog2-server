// @flow strict
import Reflux from 'reflux';
import Immutable from 'immutable';
import { get, isEqualWith } from 'lodash';

import ViewGenerator from 'enterprise/logic/views/ViewGenerator';
import SearchTypesGenerator from 'enterprise/logic/searchtypes/SearchTypesGenerator';
import { QueriesActions } from 'enterprise/actions/QueriesActions';
import View from 'enterprise/logic/views/View';
import DashboardState from 'enterprise/logic/views/DashboardState';
import Search from 'enterprise/logic/search/Search';
import ViewState from 'enterprise/logic/views/ViewState';
import type { Properties } from 'enterprise/logic/views/View';
import type { QuerySet } from 'enterprise/logic/search/Search';
import Query from 'enterprise/logic/queries/Query';

type ViewStoreState = {
  activeQuery: string,
  view: View,
  dirty: boolean,
};

type ViewActionsType = {
  create: () => Promise<ViewStoreState>,
  dashboardState: (DashboardState) => Promise<void>,
  description: (string) => Promise<void>,
  load: (View) => Promise<ViewStoreState>,
  properties: (Properties) => Promise<void>,
  search: (Search) => Promise<View>,
  selectQuery: (string) => Promise<void>,
  state: (ViewState) => Promise<View>,
  summary: (string) => Promise<void>,
  title: (string) => Promise<void>,
};

export const ViewActions: ViewActionsType = Reflux.createActions({
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
});

type ViewStoreUnsubscribe = () => void;

type ViewStoreType = {
  listen: ((ViewStoreState) => void) => ViewStoreUnsubscribe;
};

export const ViewStore: ViewStoreType = Reflux.createStore({
  listenables: [ViewActions],
  view: undefined,
  activeQuery: undefined,
  dirty: false,

  init() {
    QueriesActions.create.listen(this.createQuery);
  },

  getInitialState(): ViewStoreState {
    return this._state();
  },

  create() {
    const view = this._updateSearch(ViewGenerator());
    this.view = view;
    this.dirty = false;

    const queries: QuerySet = get(view, 'search.queries', Immutable.Set());
    const firstQueryId = queries.first().id;
    this.activeQuery = firstQueryId;

    ViewActions.create.promise(Promise.resolve(this._state()));
    this._trigger();
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
    this.view = this._updateSearch(this.view.toBuilder()
      .state(newState)
      .search(newSearch)
      .build());
    this.activeQuery = query.id;
    this._trigger();

    QueriesActions.create.promise(Promise.resolve(this.view));
  },
  dashboardState(newDashboardState: DashboardState) {
    this.dirty = true;
    this.view = this.view.toBuilder().dashboardState(newDashboardState).build();
    this._trigger();
  },
  description(newDescription: string) {
    this.dirty = true;
    this.view = this.view.toBuilder().description(newDescription).build();
    this._trigger();
  },
  load(view: View) {
    this.view = this._updateSearch(view);
    this.dirty = false;

    /* Select selected query (activeQuery) or first query in view (for now).
       Selected query might become a property on the view later. */
    const queries = get(view, 'search.queries', Immutable.List());
    const firstQueryId = queries.first().id;
    const selectedQuery = this.activeQuery && queries.find(q => (q.id === this.activeQuery)) ? this.activeQuery : firstQueryId;
    this.selectQuery(selectedQuery);

    ViewActions.load.promise(Promise.resolve(this._state()));
  },
  properties(newProperties: Properties) {
    this.dirty = true;
    this.view = this.view.toBuilder().properties(newProperties).build();
    this._trigger();
  },
  search(newSearch: Search) {
    this.dirty = true;
    this.view = this.view.toBuilder().search(newSearch).build();
    this._trigger();
    ViewActions.search.promise(Promise.resolve(this.view));
  },
  selectQuery(queryId) {
    this.activeQuery = queryId;
    this._trigger();
  },
  state(newState: ViewState) {
    this.dirty = true;
    this.view = this._updateSearch(this.view.toBuilder().state(newState).build());
    this._trigger();
    ViewActions.state.promise(Promise.resolve(this.view));
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
  _updateSearch(view: View): View {
    if (!view.search) {
      return view;
    }
    const oldWidgets = get(this.view, 'state') && this.view.state.map(s => s.widgets);
    const newWidgets = get(view, 'state') && view.state.map(s => s.widgets);
    if (!isEqualWith(oldWidgets, newWidgets, Immutable.is)) {
      let newView = view;
      const states = newView.state;
      const searchTypes = states.map(state => SearchTypesGenerator(state.widgets));

      const search = get(newView, 'search');
      const newQueries = search.queries.map(q => q.toBuilder().searchTypes(searchTypes.get(q.id, {}).searchTypes).build());
      const newSearch = search.toBuilder().queries(newQueries).build();
      newView = newView.toBuilder().search(newSearch).build();

      searchTypes.map(({ widgetMapping }) => widgetMapping)
        .forEach((widgetMapping, queryId) => {
          const newStates = newView.state;
          if (states.has(queryId)) {
            newView = newView.toBuilder().state(newStates.update(queryId, state => state.toBuilder().widgetMapping(widgetMapping).build())).build();
          }
        });
      return newView;
    }

    return view;
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
});
