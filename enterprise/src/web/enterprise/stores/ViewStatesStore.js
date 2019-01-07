import Reflux from 'reflux';
import Immutable from 'immutable';
import { get, isEqualWith } from 'lodash';

import { ViewActions, ViewStore } from './ViewStore';
import { QueriesActions } from './QueriesStore';

export const ViewStatesActions = Reflux.createActions({
  add: { asyncResult: true },
  remove: { asyncResult: true },
  update: { asyncResult: true },
});

export const ViewStatesStore = Reflux.createStore({
  listenables: [ViewStatesActions],
  states: Immutable.Map(),

  init() {
    this.listenTo(ViewStore, this.onViewStoreChange, this.onViewStoreChange);
    // Remove View State when Query is removed.
    QueriesActions.remove.listen(this.remove);
  },
  getInitialState() {
    return this._state();
  },
  onViewStoreChange({ view }) {
    const states = get(view, 'state', Immutable.Map());
    if (!isEqualWith(states, this.states, Immutable.is)) {
      this.states = states;
      this._trigger();
    }
  },
  add(queryId, viewState) {
    const newState = this.states.updateIn([queryId], (value) => {
      if (value !== undefined) {
        throw new Error(`Unable to add view state for id <${queryId}>, it is already present.`);
      }
      return viewState;
    });
    ViewActions.state(newState);
  },
  remove(queryId) {
    const newState = this.states.remove(queryId);
    ViewActions.state(newState);
  },
  update(queryId, viewState) {
    const newState = this.states.set(queryId, viewState);
    const promise = ViewActions.state(newState);
    ViewStatesActions.update.promise(promise);
  },
  _state() {
    return this.states;
  },
  _trigger() {
    this.trigger(this._state());
  },
});
