import Reflux from 'reflux';
import Immutable from 'immutable';
import { isEqual, maxBy } from 'lodash';

import { widgetDefinition } from 'enterprise/logic/Widget';

import { ViewStore } from './ViewStore';
import { ViewStatesActions, ViewStatesStore } from './ViewStatesStore';
import WidgetPosition from '../logic/widgets/WidgetPosition';

export const CurrentViewStateActions = Reflux.createActions([
  'fields',
  'titles',
  'widgets',
  'widgetPositions',
]);

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
    ViewStatesActions.update(this.activeQuery, newActiveState);
  },

  titles(newTitles) {
    const newActiveState = this._activeState().toBuilder().titles(newTitles).build();
    ViewStatesActions.update(this.activeQuery, newActiveState);
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
    ViewStatesActions.update(this.activeQuery, newActiveState);
  },

  widgetPositions(newPositions) {
    const newActiveState = this._activeState().toBuilder().widgetPositions(newPositions).build();
    ViewStatesActions.update(this.activeQuery, newActiveState);
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
