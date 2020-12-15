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
import { isEqual } from 'lodash';

import type { RefluxActions, Store } from 'stores/StoreTypes';
import FormattingSettings from 'views/logic/views/formatting/FormattingSettings';
import Widget from 'views/logic/widgets/Widget';
import { singletonActions, singletonStore } from 'views/logic/singleton';
import AddNewWidgetsToPositions from 'views/logic/views/AddNewWidgetsToPositions';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';

import { ViewStore } from './ViewStore';
import { ViewStatesActions, ViewStatesStore } from './ViewStatesStore';
import type { TitleType } from './TitleTypes';

import ViewState from '../logic/views/ViewState';

type CurrentViewStateActionsType = RefluxActions<{
  fields: (fields: Immutable.Set<string>) => Promise<unknown>,
  formatting: (formatting: FormattingSettings) => Promise<unknown>,
  titles: (titles: Immutable.Map<TitleType, Immutable.Map<string, string>>) => Promise<unknown>,
  widgets: (widgets: Immutable.List<Widget>) => Promise<unknown>,
  widgetPositions: (newPositions: Record<string, WidgetPosition>) => Promise<unknown>,
}>;

export const CurrentViewStateActions: CurrentViewStateActionsType = singletonActions(
  'views.CurrentViewState',
  () => Reflux.createActions({
    fields: { asyncResult: true },
    formatting: { asyncResult: true },
    titles: { asyncResult: true },
    widgets: { asyncResult: true },
    widgetPositions: { asyncResult: true },
  }),
);

type CurrentViewStateStoreState = {
  activeQuery: string;
  state: ViewState;
};
export type CurrentViewStateStoreType = Store<CurrentViewStateStoreState>;

export const CurrentViewStateStore: CurrentViewStateStoreType = singletonStore(
  'views.CurrentViewState',
  () => Reflux.createStore({
    listenables: [CurrentViewStateActions],
    states: Immutable.Map(),
    activeQuery: undefined,

    init() {
      this.listenTo(ViewStore, this.onViewStoreChange, this.onViewStoreChange);
      this.listenTo(ViewStatesStore, this.onViewStatesStoreChange, this.onViewStatesStoreChange);
    },

    getInitialState() {
      return this._state();
    },

    onViewStoreChange(state) {
      const { activeQuery, view } = state;

      this.view = view;

      if (!isEqual(activeQuery, this.activeQuery)) {
        this.activeQuery = activeQuery;
        this._trigger();
      }
    },

    onViewStatesStoreChange(states) {
      const activeState = this.states.get(this.activeQuery);
      const newActiveState = states.get(this.activeQuery);

      this.states = states;

      if (!isEqual(activeState, newActiveState)) {
        this._trigger();
      }
    },

    fields(newFields) {
      const newActiveState = this._activeState().toBuilder().fields(newFields).build();
      const promise = ViewStatesActions.update(this.activeQuery, newActiveState);

      CurrentViewStateActions.fields.promise(promise);
    },

    titles(newTitles) {
      const newActiveState = this._activeState().toBuilder().titles(newTitles).build();
      const promise = ViewStatesActions.update(this.activeQuery, newActiveState);

      CurrentViewStateActions.titles.promise(promise);
    },

    widgets(nextWidgets) {
      const positionsMap = Immutable.Map<string, WidgetPosition>(this._activeState().widgetPositions);
      const nextWidgetIds = nextWidgets.map(({ id }) => id);
      const cleanedPositionsMap = positionsMap.filter((_, widgetId) => nextWidgetIds.includes(widgetId)).toMap();
      const newPositionMap = AddNewWidgetsToPositions(cleanedPositionsMap, nextWidgets);

      const newActiveState = this._activeState().toBuilder()
        .widgets(nextWidgets)
        .widgetPositions(newPositionMap.toObject())
        .build();
      const promise = ViewStatesActions.update(this.activeQuery, newActiveState);

      CurrentViewStateActions.widgets.promise(promise);
    },

    widgetPositions(newPositions) {
      const newActiveState = this._activeState().toBuilder().widgetPositions(newPositions).build();
      const promise = ViewStatesActions.update(this.activeQuery, newActiveState);

      CurrentViewStateActions.widgetPositions.promise(promise);
    },

    formatting(formatting) {
      const newActiveState = this._activeState().toBuilder().formatting(formatting).build();
      const promise = ViewStatesActions.update(this.activeQuery, newActiveState);

      CurrentViewStateActions.formatting.promise(promise);
    },

    _activeState(): ViewState {
      return this.states.get(this.activeQuery);
    },

    _state() {
      return {
        state: this._activeState(),
        activeQuery: this.activeQuery,
      };
    },

    _trigger() {
      this.trigger(this._state());
    },
  }),
);
