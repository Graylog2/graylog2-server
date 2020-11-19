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
import * as Immutable from 'immutable';
import { get, isEqualWith } from 'lodash';

import type { RefluxActions } from 'stores/StoreTypes';
import type { QueryId } from 'views/logic/queries/Query';
import ViewState from 'views/logic/views/ViewState';
import { singletonActions, singletonStore } from 'views/logic/singleton';

import { ViewActions, ViewStore } from './ViewStore';

type ViewStatesActionsTypes = RefluxActions<{
  add: (QueryId, ViewState) => Promise<ViewState>,
  duplicate: (QueryId) => Promise<ViewState>,
  remove: (QueryId) => Promise<ViewState>,
  update: (QueryId, ViewState) => Promise<ViewState>,
}>;

export const ViewStatesActions: ViewStatesActionsTypes = singletonActions(
  'views.ViewStates',
  () => Reflux.createActions({
    add: { asyncResult: true },
    duplicate: { asyncResult: true },
    remove: { asyncResult: true },
    update: { asyncResult: true },
  }),
);

export const ViewStatesStore = singletonStore(
  'views.ViewStates',
  () => Reflux.createStore({
    listenables: [ViewStatesActions],
    states: Immutable.Map(),

    init() {
      this.listenTo(ViewStore, this.onViewStoreChange, this.onViewStoreChange);
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
    add(queryId: QueryId, viewState: ViewState) {
      const newState = this.states.updateIn([queryId], (value) => {
        if (value !== undefined) {
          throw new Error(`Unable to add view state for id <${queryId}>, it is already present.`);
        }

        return viewState;
      });
      const promise = ViewActions.state(newState).then(() => viewState);

      ViewStatesActions.add.promise(promise);

      return promise;
    },
    duplicate(oldQueryId: QueryId) {
      const newViewState = this.states.get(oldQueryId).duplicate();
      const promise = Promise.resolve(newViewState);

      ViewStatesActions.duplicate.promise(promise);

      return promise;
    },
    remove(queryId: QueryId) {
      const oldState = this.states.get(queryId);
      const newState = this.states.remove(queryId);
      const promise = ViewActions.state(newState).then(() => oldState);

      ViewStatesActions.remove.promise(promise);

      return promise;
    },
    update(queryId: QueryId, viewState: ViewState) {
      const newState = this.states.set(queryId, viewState);
      const promise = ViewActions.state(newState).then(() => viewState);

      ViewStatesActions.update.promise(promise);

      return promise;
    },
    _state() {
      return this.states;
    },
    _trigger() {
      this.trigger(this._state());
    },
  }),
);
