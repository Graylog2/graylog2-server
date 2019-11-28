// @flow strict
import * as Immutable from 'immutable';

import type { Color } from 'views/logic/views/formatting/highlighting/HighlightingRule';

type InternalState = {
  chartColors: { [string]: Color },
};

export type WidgetFormattingSettingsJSON = {
  chart_colors: { [string]: Color },
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

    return {
      chart_colors: chartColors,
    };
  }

  static fromJSON(value: WidgetFormattingSettingsJSON) {
    const { chart_colors: chartColors } = value;
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
