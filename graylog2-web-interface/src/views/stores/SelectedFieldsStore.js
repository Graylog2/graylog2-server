// @flow strict
import Reflux from 'reflux';
import { Set } from 'immutable';
import { get, isEqual } from 'lodash';

import type ViewState from 'views/logic/views/ViewState';
import { singletonActions, singletonStore } from 'views/logic/singleton';
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

export const SelectedFieldsStore = singletonStore(
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
      const selectedFields = Set(get(newState, 'state.fields'));
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
