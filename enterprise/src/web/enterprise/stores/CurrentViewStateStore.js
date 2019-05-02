// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';
import { isEqual, maxBy } from 'lodash';

import { widgetDefinition } from 'enterprise/logic/Widget';
import WidgetPosition from 'enterprise/logic/widgets/WidgetPosition';
import FormattingSettings from 'enterprise/logic/views/formatting/FormattingSettings';
import Widget from 'enterprise/logic/widgets/Widget';

import { ViewStore } from './ViewStore';
import { ViewStatesActions, ViewStatesStore } from './ViewStatesStore';
import type { TitleType } from './TitleTypes';

type CurrentViewStateActionsType = {
  fields: (Immutable.Set<string>) => Promise<*>,
  formatting: (FormattingSettings) => Promise<*>,
  titles: (Immutable.Map<TitleType, Immutable.Map<string, string>>) => Promise<*>,
  widgets: (Immutable.List<Widget>) => Promise<*>,
  widgetPositions: () => Promise<*>,
};

export const CurrentViewStateActions: CurrentViewStateActionsType = Reflux.createActions({
  fields: { asyncResult: true },
  formatting: { asyncResult: true },
  titles: { asyncResult: true },
  widgets: { asyncResult: true },
  widgetPositions: { asyncResult: true },
});

export const CurrentViewStateStore = Reflux.createStore({
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

  widgets(newWidgets) {
    const positionsMap = this._activeState().widgetPositions;
    const widgetsWithoutPositions = newWidgets.filter((widget) => {
      return !positionsMap[widget.id];
    });

    widgetsWithoutPositions.forEach((widget) => {
      const widgetDef = widgetDefinition(widget.type);
      const positions = Object.keys(positionsMap).map(id => positionsMap[id]);
      const lastPosition = maxBy(positions, (p) => { return p.row; });
      const newWidgetRow = lastPosition ? lastPosition.row + lastPosition.height : 1;

      positionsMap[widget.id] = new WidgetPosition(1, newWidgetRow, widgetDef.defaultHeight, widgetDef.defaultWidth);
    });

    const newActiveState = this._activeState().toBuilder()
      .widgets(newWidgets)
      .widgetPositions(positionsMap)
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

  _activeState() {
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
});
