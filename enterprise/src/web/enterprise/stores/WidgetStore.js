import Reflux from 'reflux';
import Immutable from 'immutable';
import uuid from 'uuid/v4';
import { get, isEqual } from 'lodash';

import { CurrentViewStateActions, CurrentViewStateStore } from './CurrentViewStateStore';

export const WidgetActions = Reflux.createActions({
  create: { asyncResult: true },
  duplicate: { asyncResult: true },
  filter: { asyncResult: true },
  load: { asyncResult: true },
  remove: { asyncResult: true },
  update: { asyncResult: true },
  updateConfig: { asyncResult: true },
});

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
    const newWidgets = this.widgets.set(widget.id, widget);
    this._updateWidgets(newWidgets);
    WidgetActions.create.promise(Promise.resolve(widget));
    return widget;
  },
  duplicate(widgetId) {
    const widget = this.widgets.get(widgetId);
    if (!widget) {
      throw new Error(`Unable to duplicate widget with id "${widgetId}", it is not found.`);
    }
    const duplicatedWidget = widget.duplicate(uuid());
    const newWidgets = this.widgets.set(duplicatedWidget, duplicatedWidget);
    this._updateWidgets(newWidgets);
    WidgetActions.duplicate.promise(Promise.resolve(duplicatedWidget));
    return duplicatedWidget;
  },
  filter(widgetId, filter) {
    const newWidgets = this.widgets.update(widgetId, widget => widget.toBuilder().filter(filter).build());
    this._updateWidgets(newWidgets);
  },
  remove(widgetId) {
    const newWidgets = this.widgets.remove(widgetId);
    this._updateWidgets(newWidgets);
  },
  update(widgetId, widget) {
    const newWidgets = this.widgets.set(widgetId, widget);
    this._updateWidgets(newWidgets);
  },
  updateConfig(widgetId, config) {
    const newWidgets = this.widgets.update(widgetId, widget => widget.toBuilder().config(config).build());
    this._updateWidgets(newWidgets);
  },
  _updateWidgets(newWidgets) {
    const widgets = newWidgets.valueSeq().toList();
    CurrentViewStateActions.widgets(widgets);
  },
  _trigger() {
    this.trigger(this.widgets);
  },
});
