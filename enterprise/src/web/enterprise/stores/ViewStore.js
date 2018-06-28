import Reflux from 'reflux';
import Immutable from 'immutable';
import { get, isEqualWith } from 'lodash';
import ViewGenerator from 'enterprise/logic/views/ViewGenerator';
import SearchTypesGenerator from 'enterprise/logic/searchtypes/SearchTypesGenerator';
import QueryGenerator from 'enterprise/logic/queries/QueryGenerator';
import { QueriesActions } from 'enterprise/actions/QueriesActions';

export const ViewActions = Reflux.createActions({
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

export const ViewStore = Reflux.createStore({
  listenables: [ViewActions],
  view: undefined,
  activeQuery: undefined,
  dirty: false,

  init() {
    QueriesActions.create.listen(this.createQuery);
  },

  getInitialState() {
    return this._state();
  },

  create() {
    const view = this._updateSearch(ViewGenerator());
    this.view = view;
    this.dirty = false;

    const queries = get(view, 'search.queries', Immutable.List());
    const firstQueryId = queries.first().id;
    this.activeQuery = firstQueryId;

    ViewActions.create.promise(Promise.resolve(this._state()));
    this._trigger();
  },
  createQuery(query = QueryGenerator(), viewState) {
    if (query.id === undefined) {
      throw new Error('Unable to add query without id to view.');
    }

    const { search } = this.view;

    const newQueries = search.queries.push(query);

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
  dashboardState(newDashboardState) {
    this.dirty = true;
    this.view = this.view.toBuilder().dashboardState(newDashboardState).build();
    this._trigger();
  },
  description(newDescription) {
    this.dirty = true;
    this.view = this.view.toBuilder().description(newDescription).build();
    this._trigger();
  },
  load(view) {
    this.view = this._updateSearch(view);
    this.dirty = false;

    /* Select selected query (activeQuery) or first query in view (for now).
       Selected query might become a property on the view later. */
    const queries = get(view, 'search.queries', Immutable.List());
    const firstQueryId = queries.first().id;
    this.selectQuery(this.activeQuery || firstQueryId);

    ViewActions.load.promise(Promise.resolve(this._state()));
  },
  properties(newProperties) {
    this.dirty = true;
    this.view = this.view.toBuilder().properties(newProperties).build();
    this._trigger();
  },
  search(newSearch) {
    this.dirty = true;
    this.view = this.view.toBuilder().search(newSearch).build();
    this._trigger();
    ViewActions.search.promise(Promise.resolve(this.view));
  },
  selectQuery(queryId) {
    this.activeQuery = queryId;
    this._trigger();
  },
  state(newState) {
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
  _updateSearch(view) {
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
  _state() {
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