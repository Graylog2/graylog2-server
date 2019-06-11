// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';
import { get, isEqual } from 'lodash';

import Widget from 'enterprise/logic/widgets/Widget';
import { WidgetActions } from 'enterprise/stores/WidgetStore';
import WidgetFormattingSettings from 'enterprise/logic/aggregationbuilder/WidgetFormattingSettings';

import { WidgetStore } from './WidgetStore';

type Color = string;

export type ColorRule = {
  widgetId: string,
  name: string,
  color: Color,
};

type ChartColorRulesActionsType = {
  set: (widgetId: string, name: string, color: Color) => Promise<Array<ColorRule>>,
};

const ChartColorRulesActions: ChartColorRulesActionsType = Reflux.createActions({
  set: { asyncResult: true },
});

type Key = {
  widgetId: string,
  name: string,
};

type Value = Color;

const ChartColorRulesStore = Reflux.createStore({
  listenables: [ChartColorRulesActions],

  widgets: Immutable.Map<string, Widget>(),
  state: Immutable.Map<Key, Value>(),

  init() {
    this.listenTo(WidgetStore, this.onWidgetStoreChange, this.onWidgetStoreChange);
  },

  onWidgetStoreChange(newState: Immutable.Map<string, Widget>) {
    this.widgets = newState;
    const newRules = newState.valueSeq().map((widget) => {
      const { config } = widget;
      const widgetId = widget.id;
      if (config.formattingSettings) {
        const { chartColors = {} } = config.formattingSettings;
        return Object.entries(chartColors).map(([key, value]) => ({ widgetId, name: key, color: value }));
      }
      return null;
    }).filter(rules => (rules !== null))
      .reduce((prev, cur) => [...prev, ...cur], []);

    if (!isEqual(this.state, newRules)) {
      this.state = newRules;
      this._trigger();
    }
  },

  getInitialState(): Array<ColorRule> {
    return this._state();
  },
  _state(): Array<ColorRule> {
    return this.state;
  },
  set(widgetId: string, name: string, color: Color): Promise<*> {
    const widget = this.widgets.get(widgetId);
    const formattingSettings: WidgetFormattingSettings = get(widget, ['config', 'formattingSettings'], WidgetFormattingSettings.empty());
    const { chartColors } = formattingSettings;

    const newWidget = widget.toBuilder()
      .config(widget.config.toBuilder()
        .formattingSettings(formattingSettings.toBuilder()
          .chartColors({ ...chartColors, [name]: color })
          .build(),
        ).build(),
      ).build();

    const promise = WidgetActions.update(widgetId, newWidget);
    ChartColorRulesActions.set.promise(promise);
    return promise;
  },
  _trigger() {
    this.trigger(this._state());
  },
});

export { ChartColorRulesActions, ChartColorRulesStore };
