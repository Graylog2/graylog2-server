import Reflux from 'reflux';
import Immutable from 'immutable';
import { get, isEqual } from 'lodash';

import { ViewActions, ViewStore } from './ViewStore';
import { WidgetActions } from './WidgetStore';
import DashboardWidget from '../logic/views/DashboardWidget';

export const DashboardWidgetsActions = Reflux.createActions([
  'addToDashboard',
  'removeFromDashboard',
  'positions',
]);

export const DashboardWidgetsStore = Reflux.createStore({
  listenables: [DashboardWidgetsActions],

  widgets: Immutable.Map(),
  dashboardState: undefined,
  allWidgets: Immutable.Map(),

  init() {
    this.listenTo(ViewStore, this.onViewStoreChange, this.onViewStoreChange);
    WidgetActions.remove.listen(this.removeFromDashboard);
  },

  getInitialState() {
    return this._state();
  },

  onViewStoreChange({ view }) {
    this.dashboardState = get(view, 'dashboardState');
    const newWidgets = get(this.dashboardState, 'widgets', Immutable.Map());
    const newPositions = get(this.dashboardState, 'positions', Immutable.Map());
    const allWidgets = Immutable.Map(
      get(view, 'state', Immutable.Map())
        .valueSeq()
        .map(s => Immutable.fromJS(s.widgets))
        .flatten()
        .map(w => [w.id, w])
        .toList(),
    );
    if (!isEqual(newWidgets, this.widgets) || !isEqual(this.allWidgets, allWidgets) || !isEqual(this._positions, newPositions)) {
      this.widgets = newWidgets;
      this.allWidgets = allWidgets;
      this._positions = newPositions;
      this._trigger();
    }
  },

  _state() {
    return {
      dashboardWidgets: this.widgets,
      widgetDefs: this.allWidgets,
      positions: this._positions,
    };
  },
  _trigger() {
    this.trigger(this._state());
  },

  addToDashboard(queryId, widgetId) {
    const newDashboardState = this.dashboardState.toBuilder()
      .widgets(this.widgets.set(widgetId, DashboardWidget.create(widgetId, queryId)))
      .build();
    ViewActions.dashboardState(newDashboardState);
  },

  positions(newPositions) {
    const newDashboardState = this.dashboardState.toBuilder()
      .positions(Immutable.fromJS(newPositions))
      .build();
    ViewActions.dashboardState(newDashboardState);
  },

  removeFromDashboard(widgetId) {
    const newDashboardState = this.dashboardState.toBuilder()
      .widgets(this.widgets.delete(widgetId))
      .build();
    ViewActions.dashboardState(newDashboardState);
  },
});
