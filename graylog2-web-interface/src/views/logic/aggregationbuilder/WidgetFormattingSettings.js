// @flow strict
import * as Immutable from 'immutable';

import type { Color } from 'views/logic/views/formatting/highlighting/HighlightingRule';

type ChartColors = { [string]: Color };

type InternalState = {
  chartColors: ChartColors,
};

type ChartColorSettingJson = {
  field_name: string,
  chart_color: Color,
};

export type WidgetFormattingSettingsJSON = {
  chart_colors: Array<ChartColorSettingJson>,
};

export default class WidgetFormattingSettings {
  _value: InternalState;

  // eslint-disable-next-line no-undef
  constructor(chartColors: $PropertyType<InternalState, 'chartColors'>) {
    this._value = { chartColors };
  }

  get chartColors() {
    return this._value.chartColors;
  }

  toBuilder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }

  // eslint-disable-next-line no-undef
  static create(chartColors: $PropertyType<InternalState, 'chartColors'>) {
    return new WidgetFormattingSettings(chartColors);
  }

  static builder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder()
      .chartColors({});
  }

  static empty() {
    return WidgetFormattingSettings.builder().build();
  }

  toJSON() {
    const { chartColors } = this._value;
    // $FlowFixMe flow cannot handle Object.keys
    const chartColorJson = Object.keys(chartColors)
      .map(fieldName => ({ field_name: fieldName, chart_color: chartColors[fieldName] }));
    return { chart_colors: chartColorJson };
  }

  static fromJSON(value: WidgetFormattingSettingsJSON) {
    const { chart_colors: chartColorJson } = value;
    const chartColors: ChartColors = chartColorJson.reduce((acc, { field_name: fieldName, chart_color: chartColor }) => {
      acc[fieldName] = chartColor;
      return acc;
    }, {});
    return WidgetFormattingSettings.create(chartColors);
  }
}

type BuilderState = Immutable.Map<string, any>;

class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  chartColors(value: $PropertyType<InternalState, 'chartColors'>) {
    return new Builder(this.value.set('chartColors', value));
  }

  build() {
    const { chartColors } = this.value.toObject();
    return new WidgetFormattingSettings(chartColors);
  }
}
