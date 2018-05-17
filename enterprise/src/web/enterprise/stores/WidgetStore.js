import Reflux from 'reflux';
import Immutable from 'immutable';
import uuid from 'uuid/v4';
import { get, isEqual } from 'lodash';

import { CurrentViewStateActions, CurrentViewStateStore } from './CurrentViewStateStore';

export const WidgetActions = Reflux.createActions([
  'create',
  'duplicate',
  'filter',
  'load',
  'remove',
  'update',
  'updateConfig',
]);

export const WidgetStore = Reflux.createStore({
  listenables: [WidgetActions],

  widgets: new Immutable.Map(),
  activeQuery: undefined,

  init() {
    this.listenTo(CurrentViewStateStore, this.onCurrentViewStateChange, this.onCurrentViewStateChange);
  },

  getInitialState() {
    return this.widgets;
  },

  onCurrentViewStateChange(newState) {
    const { activeQuery, state } = newState;
    this.activeQuery = activeQuery;

    const activeWidgets = get(state, 'widgets', []);
    const widgets = Immutable.Map(activeWidgets.map(w => [w.id, w]));

    if (!isEqual(widgets, this.widgets)) {
      this.widgets = Immutable.Map(widgets);
      this._trigger();
    }
  },

  create(widget) {
    if (widget.id === undefined) {
      throw new Error('Unable to add widget without id to query.');
    }
    this.widgets = this.widgets.set(widget.id, widget);
    this._updateWidgets();
  },
  duplicate(widgetId) {
    const widget = this.widgets.get(widgetId);
    if (!widget) {
      throw new Error(`Unable to duplicate widget with id "${widgetId}", it is not found.`);
    }
    const duplicatedWidget = widget.duplicate(uuid());
    this.widgets = this.widgets.set(duplicatedWidget, duplicatedWidget);
    this._updateWidgets();
    return duplicatedWidget;
  },
  filter(widgetId, filter) {
    this.widgets = this.widgets.update(widgetId, widget => widget.toBuilder().filter(filter).build());
    this._updateWidgets(this.widgets);
  },
  remove(widgetId) {
    this.widgets = this.widgets.remove(widgetId);
    this._updateWidgets();
  },
  update(widgetId, widget) {
    this.widgets = this.widgets.set(widgetId, widget);
    this._updateWidgets();
  },
  updateConfig(widgetId, config) {
    this.widgets = this.widgets.update(widgetId, widget => widget.toBuilder().config(config).build());
    this._updateWidgets();
  },
  _updateWidgets() {
    const widgets = this.widgets.valueSeq().toList();
    CurrentViewStateActions.widgets(widgets);
  },
  _trigger() {
    this.trigger(this.widgets);
  },
});
