import Reflux from 'reflux';
import Immutable from 'immutable';
import { get } from 'lodash';

import { singletonStore } from 'views/logic/singleton';
import { ViewStore } from './ViewStore';

export default singletonStore(
  'views.CurrentDashboardTitles',
  () => Reflux.createStore({
    titles: new Immutable.Map(),

    init() {
      this.listenTo(ViewStore, this.onViewStoreChange, this.onViewStoreChange);
    },

    getInitialState() {
      return this.titles;
    },

    onViewStoreChange({ view }) {
      const state = get(view, 'state', Immutable.Map());
      // We need the widget titles of all queries on a dashboard
      state.valueSeq().forEach((s) => {
        this.titles = this.titles.mergeDeep(s.titles);
      });
      this.trigger(this.titles);
    },
  }),
);
