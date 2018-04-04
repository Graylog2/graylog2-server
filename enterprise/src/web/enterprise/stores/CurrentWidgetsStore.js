import Reflux from 'reflux';
import Immutable from 'immutable';

import CurrentWidgetsActions from 'enterprise/actions/CurrentWidgetsActions';
import WidgetActions from '../actions/WidgetActions';
import CurrentViewStore from './CurrentViewStore';

export default Reflux.createStore({
  listenables: [CurrentWidgetsActions],
  state: new Immutable.Map(),
  selectedView: undefined,
  selectedQuery: undefined,

  init() {
    this.listenTo(CurrentViewStore, this.onCurrentViewStoreChange, this.onCurrentViewStoreChange);
  },

  onCurrentViewStoreChange(state) {
    if (this.selectedView !== state.selectedView) {
      this.selectedView = state.selectedView;
    }
    if (this.selectedQuery !== state.selectedQuery) {
      this.selectedQuery = state.selectedQuery;
    }
  },

  duplicate(widgetId) {
    const promise = WidgetActions.duplicate.triggerPromise(this.selectedView, this.selectedQuery, widgetId);
    CurrentWidgetsActions.duplicate.promise(promise);
    return promise;
  },

  updateConfig(widgetId, newConfig) {
    const promise = WidgetActions.updateConfig.triggerPromise(this.selectedView, this.selectedQuery, widgetId, newConfig);
    CurrentWidgetsActions.updateConfig.promise(promise);
    return promise;
  },

  remove(widgetId) {
    const promise = WidgetActions.remove.triggerPromise(this.selectedView, this.selectedQuery, widgetId);
    CurrentWidgetsActions.remove.promise(promise);
    return promise;
  }
});