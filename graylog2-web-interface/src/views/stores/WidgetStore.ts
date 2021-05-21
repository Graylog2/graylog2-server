/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import Reflux from 'reflux';
import * as Immutable from 'immutable';
import uuid from 'uuid/v4';
import { get, isEqual } from 'lodash';

import type { RefluxActions, Store } from 'stores/StoreTypes';
import Widget from 'views/logic/widgets/Widget';
import { singletonActions, singletonStore } from 'views/logic/singleton';
import type { QueryString, TimeRange } from 'views/logic/queries/Query';

import { CurrentViewStateActions, CurrentViewStateStore } from './CurrentViewStateStore';

type WidgetId = string;

export type Widgets = Immutable.OrderedMap<string, Widget>;

type WidgetActionsType = RefluxActions<{
  create: (widget: Widget) => Promise<Widget>,
  duplicate: (widgetId: WidgetId) => Promise<Widget>,
  filter: (widgetId: WidgetId, filter: string) => Promise<Widgets>,
  timerange: (widgetId: WidgetId, timerange: TimeRange) => Promise<Widgets>,
  query: (widgetId: WidgetId, queryString: QueryString) => Promise<Widgets>,
  streams: (widgetId: WidgetId, streams: Array<string>) => Promise<Widgets>,
  remove: (widgetId: WidgetId) => Promise<Widgets>,
  update: (widgetId: WidgetId, widget: Widget) => Promise<Widgets>,
  updateConfig: (widgetId: WidgetId, config: any) => Promise<Widgets>,
  updateWidgets: (widgets: Immutable.Map<string, Widget>) => Promise<Widgets>,
}>;

export type WidgetStoreState = Immutable.Map<string, Widget>;

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

export const WidgetStore: Store<WidgetStoreState> = singletonStore(
  'views.Widget',
  () => Reflux.createStore({
    listenables: [WidgetActions],

    widgets: Immutable.Map(),
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
