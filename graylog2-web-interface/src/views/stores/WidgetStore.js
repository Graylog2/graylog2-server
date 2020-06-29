// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';
import uuid from 'uuid/v4';
import { get, isEqual } from 'lodash';

import type { RefluxActions } from 'stores/StoreTypes';
import Widget from 'views/logic/widgets/Widget';
import { singletonActions, singletonStore } from 'views/logic/singleton';
import type { QueryString, TimeRange } from 'views/logic/queries/Query';

import { CurrentViewStateActions, CurrentViewStateStore } from './CurrentViewStateStore';

type WidgetId = string;

type Widgets = Immutable.OrderedMap<string, Widget>;

type WidgetActionsType = RefluxActions<{
  create: (Widget) => Promise<Widget>,
  duplicate: (WidgetId) => Promise<Widget>,
  filter: (WidgetId, string) => Promise<Widgets>,
  timerange: (WidgetId, TimeRange) => Promise<Widgets>,
  query: (WidgetId, QueryString) => Promise<Widgets>,
  streams: (WidgetId, Array<string>) => Promise<Widgets>,
  remove: (WidgetId) => Promise<Widgets>,
  update: (WidgetId, Widget) => Promise<Widgets>,
  updateConfig: (WidgetId, any) => Promise<Widgets>,
  updateWidgets: (Map<string, Widget>) => Promise<Widgets>,
}>;

type WidgetStoreState = {
  widgets: Map<string, Widget>,
};

export const WidgetActions: WidgetActionsType = singletonActions(
  'views.Widget',
  () => Reflux.createActions({
    create: { asyncResult: true },
    duplicate: { asyncResult: true },
    filter: { asyncResult: true },
    timerange: { asyncResult: true },
    query: { asyncResult: true },
    streams: { asyncResult: true },
    remove: { asyncResult: true },
    update: { asyncResult: true },
    updateConfig: { asyncResult: true },
    updateWidgets: { asyncResult: true },
  }),
);

export const WidgetStore = singletonStore(
  'views.Widget',
  () => Reflux.createStore({
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
      const widgets = Immutable.OrderedMap(activeWidgets.map((w) => [w.id, w]));

      if (!isEqual(widgets, this.widgets)) {
        this.widgets = widgets;
        this._trigger();
      }
    },

    create(widget): Promise<Widget> {
      if (widget.id === undefined) {
        throw new Error('Unable to add widget without id to query.');
      }

      const newWidgets = this.widgets.set(widget.id, widget);
      const promise = this._updateWidgets(newWidgets).then(() => widget);

      WidgetActions.create.promise(promise);

      return widget;
    },
    duplicate(widgetId): Promise<Widget> {
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
    filter(widgetId, filter): Promise<Widgets> {
      const newWidgets = this.widgets.update(widgetId, (widget: Widget) => widget.toBuilder().filter(filter).build());
      const promise = this._updateWidgets(newWidgets).then(() => newWidgets);

      WidgetActions.filter.promise(promise);

      return newWidgets;
    },
    timerange(widgetId: WidgetId, timerange: TimeRange): Promise<Widgets> {
      const newWidgets = this.widgets.update(widgetId, (widget: Widget) => widget.toBuilder().timerange(timerange).build());
      const promise = this._updateWidgets(newWidgets).then(() => newWidgets);

      WidgetActions.timerange.promise(promise);

      return newWidgets;
    },
    query(widgetId: WidgetId, query: QueryString): Promise<Widgets> {
      const newWidgets = this.widgets.update(widgetId, (widget: Widget) => widget.toBuilder().query(query).build());
      const promise = this._updateWidgets(newWidgets).then(() => newWidgets);

      WidgetActions.query.promise(promise);

      return newWidgets;
    },
    streams(widgetId: WidgetId, streams: Array<string>): Promise<Widgets> {
      const newWidgets = this.widgets.update(widgetId, (widget: Widget) => widget.toBuilder().streams(streams).build());
      const promise = this._updateWidgets(newWidgets).then(() => newWidgets);

      WidgetActions.streams.promise(promise);

      return newWidgets;
    },
    remove(widgetId): Promise<Widgets> {
      const newWidgets = this.widgets.remove(widgetId);
      const promise = this._updateWidgets(newWidgets).then(() => newWidgets);

      WidgetActions.remove.promise(promise);

      return newWidgets;
    },
    update(widgetId, widget): Promise<Widgets> {
      const newWidgets = this.widgets.set(widgetId, widget);
      const promise = this._updateWidgets(newWidgets).then(() => newWidgets);

      WidgetActions.update.promise(promise);

      return newWidgets;
    },
    updateWidgets(newWidgets): Promise<Widgets> {
      const promise = this._updateWidgets(newWidgets).then(() => newWidgets);

      WidgetActions.updateWidgets.promise(promise);

      return newWidgets;
    },
    updateConfig(widgetId, config): Promise<Widgets> {
      const newWidgets = this.widgets.update(widgetId, (widget) => widget.toBuilder().config(config).build());
      const promise = this._updateWidgets(newWidgets).then(() => newWidgets);

      WidgetActions.updateConfig.promise(promise);

      return newWidgets;
    },
    _updateWidgets(newWidgets) {
      const widgets = newWidgets.valueSeq().toList();

      return CurrentViewStateActions.widgets(widgets);
    },
    _trigger() {
      this.trigger(this.widgets);
    },
  }),
);
