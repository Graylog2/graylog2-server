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
// @flow strict
import * as Immutable from 'immutable';
import { $PropertyType } from 'utility-types';

import type { Color } from 'views/logic/views/formatting/highlighting/HighlightingRule';

type ChartColors = { [key: string]: Color };

type InternalState = {
  chartColors: ChartColors,
};

/* eslint-disable camelcase */
type ChartColorSettingJson = {
  field_name: string,
  chart_color: Color,
};

export type WidgetFormattingSettingsJSON = {
  chart_colors: Array<ChartColorSettingJson>,
};
/* eslint-enable camelcase */

export default class WidgetFormattingSettings {
  private readonly _value: InternalState;

  // eslint-disable-next-line no-undef
  constructor(chartColors: $PropertyType<InternalState, 'chartColors'>) {
    this._value = { chartColors };
  }

  get chartColors() {
    return this._value.chartColors;
  }

  toBuilder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }

  // eslint-disable-next-line no-undef
  static create(chartColors: $PropertyType<InternalState, 'chartColors'>) {
    return new WidgetFormattingSettings(chartColors);
  }

  static builder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
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
      .map((fieldName) => ({ field_name: fieldName, chart_color: chartColors[fieldName] }));

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
