// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';
import { get, isEqual } from 'lodash';

import type { RefluxActions } from 'stores/StoreTypes';
import Widget from 'views/logic/widgets/Widget';
import { WidgetActions, WidgetStore } from 'views/stores/WidgetStore';
import WidgetFormattingSettings from 'views/logic/aggregationbuilder/WidgetFormattingSettings';
import { singletonActions, singletonStore } from 'views/logic/singleton';

type Color = string;

export type ColorRule = {
  widgetId: string,
  name: string,
  color: Color,
};

type ChartColorRulesActionsType = RefluxActions<{
  set: (widgetId: string, name: string, color: Color) => Promise<Array<ColorRule>>,
}>;

const ChartColorRulesActions: ChartColorRulesActionsType = singletonActions(
  'views.ChartColorRules',
  () => Reflux.createActions({
    set: { asyncResult: true },
  }),
);

type Key = {
  widgetId: string,
  name: string,
};

type Value = Color;

const ChartColorRulesStore = singletonStore(
  'views.ChartColorRules',
  () => Reflux.createStore({
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
      }).reduce((prev, cur) => (cur === null ? prev : [...prev, ...cur]), []);

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
            .build()).build()).build();

      const promise = WidgetActions.update(widgetId, newWidget);
      ChartColorRulesActions.set.promise(promise);
      return promise;
    },
    _trigger() {
      this.trigger(this._state());
    },
  }),
);

export { ChartColorRulesActions, ChartColorRulesStore };
