import Reflux from 'reflux';
import Immutable from 'immutable';

import DashboardWidgetsActions from 'enterprise/actions/DashboardWidgetsActions';
import CurrentViewStore from './CurrentViewStore';

export default Reflux.createStore({
  listenables: [DashboardWidgetsActions],

  widgets: new Immutable.Map(),

  init() {
    // TODO: Listen on widgets store to remove widgets here when they are removed from a query tab
    // this.listenTo(CurrentViewStore, this.onCurrentViewStoreChange, this.onCurrentViewStoreChange);
  },

  getInitialState() {
    return this.widgets;
  },

  _trigger() {
    this.trigger(this.widgets);
  },

  addToDashboard(viewId, queryId, widgetId) {
    this.widgets = this.widgets.setIn([viewId, widgetId], { viewId: viewId, queryId: queryId, widgetId: widgetId });
    this._trigger();
  },

  removeFromDashboard(viewId, widgetId) {
    this.widgets = this.widgets.deleteIn([viewId, widgetId]);
    this._trigger();
  },
});
