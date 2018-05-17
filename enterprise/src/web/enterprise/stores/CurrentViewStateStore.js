import Reflux from 'reflux';
import Immutable from 'immutable';
import { isEqual } from 'lodash';

import { ViewStore } from './ViewStore';
import { ViewStatesActions, ViewStatesStore } from './ViewStatesStore';

export const CurrentViewStateActions = Reflux.createActions([
  'fields',
  'titles',
  'widgets',
  'widgetPositions',
]);

export const CurrentViewStateStore = Reflux.createStore({
  listenables: [CurrentViewStateActions],
  states: Immutable.Map(),
  activeQuery: undefined,

  init() {
    this.listenTo(ViewStore, this.onViewStoreChange, this.onViewStoreChange);
    this.listenTo(ViewStatesStore, this.onViewStatesStoreChange, this.onViewStatesStoreChange);
  },

  getInitialState() {
    return this._state();
  },

  onViewStoreChange(state) {
    const { activeQuery, view } = state;

    this.view = view;

    if (!isEqual(activeQuery, this.activeQuery)) {
      this.activeQuery = activeQuery;
      this._trigger();
    }
  },

  onViewStatesStoreChange(states) {
    const activeState = this.states.get(this.activeQuery);
    const newActiveState = states.get(this.activeQuery);

    this.states = states;

    if (!isEqual(activeState, newActiveState)) {
      this._trigger();
    }
  },

  fields(newFields) {
    const newActiveState = this._activeState().toBuilder().fields(newFields).build();
    ViewStatesActions.update(this.activeQuery, newActiveState);
  },

  titles(newTitles) {
    const newActiveState = this._activeState().toBuilder().titles(newTitles).build();
    ViewStatesActions.update(this.activeQuery, newActiveState);
  },

  widgets(newWidgets) {
    const newActiveState = this._activeState().toBuilder().widgets(newWidgets).build();
    ViewStatesActions.update(this.activeQuery, newActiveState);
  },

  widgetPositions(newPositions) {
    const newActiveState = this._activeState().toBuilder().widgetPositions(newPositions).build();
    ViewStatesActions.update(this.activeQuery, newActiveState);
  },

  _activeState() {
    return this.states.get(this.activeQuery);
  },

  _state() {
    return {
      state: this._activeState(),
      activeQuery: this.activeQuery,
    };
  },

  _trigger() {
    this.trigger(this._state());
  },
});
