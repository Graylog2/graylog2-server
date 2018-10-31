import Reflux from 'reflux';
import Immutable from 'immutable';
import { get, isEqual } from 'lodash';

import { CurrentViewStateActions, CurrentViewStateStore } from './CurrentViewStateStore';

export const TitlesActions = Reflux.createActions([
  'set',
]);

export const TitleTypes = {
  Tab: 'tab',
  Widget: 'widget',
};

export const TitlesStore = Reflux.createStore({
  listenables: [TitlesActions],

  titles: new Immutable.Map(),

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
    CurrentViewStateActions.titles(this.titles);
    this._trigger();
  },

  _trigger() {
    this.trigger(this.titles);
  },
});
