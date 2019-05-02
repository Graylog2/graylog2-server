// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';
import { get, isEqual } from 'lodash';

import { CurrentViewStateActions, CurrentViewStateStore } from './CurrentViewStateStore';
import type { TitlesMap, TitleType } from './TitleTypes';

type TitlesActionsTypes = {
  set: (string, string, string) => Promise<TitlesMap>
};

export const TitlesActions: TitlesActionsTypes = Reflux.createActions({
  set: { asyncResult: true },
});

export { default as TitleTypes } from './TitleTypes';

export const TitlesStore = Reflux.createStore({
  listenables: [TitlesActions],

  titles: Immutable.Map<TitleType, Immutable.Map<string, string>>(),

  init() {
    this.listenTo(CurrentViewStateStore, this.onViewStateStoreChange, this.onViewStateStoreChange);
  },
  getInitialState() {
    return this.titles;
  },
  onViewStateStoreChange({ state }) {
    const titles = get(state, 'titles');
    if (!isEqual(titles, this.titles)) {
      this.titles = titles;
      this._trigger();
    }
  },

  set(type, id, title) {
    this.titles = this.titles.setIn([type, id], title);
    const promise = CurrentViewStateActions.titles(this.titles).then(() => this.titles);
    this._trigger();
    TitlesActions.set.promise(promise);
    return promise;
  },

  _trigger() {
    this.trigger(this.titles);
  },
});
