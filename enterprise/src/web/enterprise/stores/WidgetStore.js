// @flow strict
import Reflux from 'reflux';
import Immutable from 'immutable';
import uuid from 'uuid/v4';
import { get, isEqual } from 'lodash';

import Widget from '../logic/widgets/Widget';
import { CurrentViewStateActions, CurrentViewStateStore } from './CurrentViewStateStore';


type WidgetActionsType = {
  create: (Widget) => Promise<Widget>,
  duplicate: (string) => Promise<Widget>,
  filter: (string, string) => Promise<void>,
  remove: (string) => Promise<void>,
  update: (string, Widget) => Promise<void>,
  updateConfig: (string, any) => Promise<void>,
  updateWidgets: (Map<string, Widget>) => Promise<void>,
};

type WidgetStoreState = {
  widgets: Map<string, Widget>,
};

export const WidgetActions: WidgetActionsType = Reflux.createActions({
  create: { asyncResult: true },
  duplicate: { asyncResult: true },
  filter: { asyncResult: true },
  remove: { asyncResult: true },
  update: { asyncResult: true },
  updateConfig: { asyncResult: true },
  updateWidgets: { asyncResult: true },
});

export const WidgetStore = Reflux.createStore({
  listenables: [WidgetActions],

  widgets: new Immutable.Map(),
  activeQuery: undefined,

  init() {
    this.listenTo(CurrentViewStateStore, this.onCurrentViewStateChange, this.onCurrentViewStateChange);
  },

  getInitialState(): WidgetStoreState {
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
    const promise = this._updateWidgets(newWidgets).then(() => widget);
    WidgetActions.create.promise(promise);
    return widget;
  },
  duplicate(widgetId) {
    const widget = this.widgets.get(widgetId);
    if (!widget) {
      throw new Error(`Unable to duplicate widget with id "${widgetId}", it is not found.`);
    }
    const duplicatedWidget = widget.duplicate(uuid());
    const newWidgets = this.widgets.set(duplicatedWidget.id, duplicatedWidget);
    const promise = this._updateWidgets(newWidgets);
    WidgetActions.duplicate.promise(promise.then(() => duplicatedWidget));
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
  updateWidgets(newWidgets) {
    this._updateWidgets(newWidgets);
  },
  updateConfig(widgetId, config) {
    const newWidgets = this.widgets.update(widgetId, widget => widget.toBuilder().config(config).build());
    this._updateWidgets(newWidgets);
  },
  _updateWidgets(newWidgets) {
    const widgets = newWidgets.valueSeq().toList();
    return CurrentViewStateActions.widgets(widgets);
  },
  _trigger() {
    this.trigger(this.widgets);
  },
});
