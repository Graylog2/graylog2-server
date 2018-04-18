import Reflux from 'reflux';
import Immutable from 'immutable';

import TitlesStore from './TitlesStore';

export default Reflux.createStore({
  titles: new Immutable.Map(),

  init() {
    this.listenTo(TitlesStore, this.onTitlesStoreChange, this.onTitlesStoreChange);
  },

  getInitialState() {
    return this.titles;
  },

  onTitlesStoreChange(state) {
    // We need the widget titles of all queries on a dashboard
    state.valueSeq().forEach((titles) => {
      this.titles = this.titles.mergeDeep(titles);
    });
    this.trigger(this.titles);
  },
});
