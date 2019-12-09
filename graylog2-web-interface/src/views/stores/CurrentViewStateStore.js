// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';
import { isEqual } from 'lodash';

import type { RefluxActions } from 'stores/StoreTypes';
import { widgetDefinition } from 'views/logic/Widgets';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import FormattingSettings from 'views/logic/views/formatting/FormattingSettings';
import Widget from 'views/logic/widgets/Widget';
import { singletonActions, singletonStore } from 'views/logic/singleton';

import { ViewStore } from './ViewStore';
import { ViewStatesActions, ViewStatesStore } from './ViewStatesStore';
import type { TitleType } from './TitleTypes';

type CurrentViewStateActionsType = RefluxActions<{
  fields: (Immutable.Set<string>) => Promise<*>,
  formatting: (FormattingSettings) => Promise<*>,
  titles: (Immutable.Map<TitleType, Immutable.Map<string, string>>) => Promise<*>,
  widgets: (Immutable.List<Widget>) => Promise<*>,
  widgetPositions: () => Promise<*>,
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

export const CurrentViewStateStore = singletonStore(
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

    widgets(newWidgets) {
      const positionsMap = Immutable.Map(this._activeState().widgetPositions);
      const widgetsWithoutPositions = newWidgets.filter((widget) => {
        return !positionsMap.get(widget.id);
      });

      const newPositionMap = widgetsWithoutPositions.reduce((nextPositionsMap, widget) => {
        const widgetDef = widgetDefinition(widget.type);
        const result = nextPositionsMap.reduce((newPosMap, position, id) => {
          const pos = position.toBuilder().row(position.row + widgetDef.defaultHeight).build();
          return newPosMap.set(id, pos);
        }, Immutable.Map());
        return result.set(widget.id, new WidgetPosition(1, 1, widgetDef.defaultHeight, widgetDef.defaultWidth));
      }, positionsMap);

      const newActiveState = this._activeState().toBuilder()
        .widgets(newWidgets)
        .widgetPositions(newPositionMap.toObject())
        .build();
      const promise = ViewStatesActions.update(this.activeQuery, newActiveState);
      CurrentViewStateActions.widgets.promise(promise);
      return promise;
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
  }),
);
