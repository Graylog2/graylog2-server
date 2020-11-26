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
import { Set } from 'immutable';
import { get, isEqual } from 'lodash';

import type ViewState from 'views/logic/views/ViewState';
import { singletonActions, singletonStore } from 'views/logic/singleton';
import { Store } from 'stores/StoreTypes';

import { CurrentViewStateActions, CurrentViewStateStore } from './CurrentViewStateStore';

type StateUpdate = {
  activeQuery: string,
  state: ViewState,
};

export const SelectedFieldsActions = singletonActions(
  'views.SelectedFields',
  () => Reflux.createActions([
    'add',
    'remove',
    'set',
  ]),
);

export type SelectedFieldsStoreState = Set<string> | undefined;

export const SelectedFieldsStore: Store<SelectedFieldsStoreState> = singletonStore(
  'views.SelectedFields',
  () => Reflux.createStore({
    listenables: [SelectedFieldsActions],
    selectedFields: undefined,

    init() {
      this.listenTo(CurrentViewStateStore, this.onViewStoreChange, this.onViewStoreChange);
    },

    getInitialState() {
      return this._state();
    },

    onViewStoreChange(newState: StateUpdate) {
      const selectedFields = Set<string>(get(newState, 'state.fields'));

      if (!isEqual(this.selectedFields, selectedFields)) {
        this.selectedFields = selectedFields;
        this._trigger();
      }
    },

    add(field: string) {
      CurrentViewStateActions.fields(this.selectedFields.add(field));
    },
    remove(field: string) {
      CurrentViewStateActions.fields(this.selectedFields.remove(field));
    },
    set(fields: Array<string>) {
      CurrentViewStateActions.fields(Set(fields));
    },
    _state() {
      return this.selectedFields;
    },
    _trigger() {
      this.trigger(this._state());
    },
  }),
);
