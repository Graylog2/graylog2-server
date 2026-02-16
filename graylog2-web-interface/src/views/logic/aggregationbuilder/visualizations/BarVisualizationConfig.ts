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
import type {
  XYVisualization,
  AxisType,
  ChartAxisConfig,
} from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import { DEFAULT_AXIS_CONFIG, DEFAULT_AXIS_TYPE } from 'views/logic/aggregationbuilder/visualizations/XYVisualization';

import VisualizationConfig from './VisualizationConfig';

export const DEFAULT_BARMODE = 'group';

export type BarMode = 'stack' | 'group' | 'overlay' | 'relative';

export type BarVisualizationConfigType = {
  barmode: BarMode;
  axisType: AxisType;
  axisConfig: ChartAxisConfig;
};

export type BarVisualizationConfigJson = {
  barmode: BarMode;
  axis_type: AxisType;
  axis_config?: ChartAxisConfig;
};

export default class BarVisualizationConfig extends VisualizationConfig implements XYVisualization {
  _value: BarVisualizationConfigType;

  constructor(barmode: BarMode, axisType: AxisType, axisConfig: ChartAxisConfig = DEFAULT_AXIS_CONFIG) {
    super();
    this._value = { barmode, axisType, axisConfig };
  }

  get barmode() {
    return this._value.barmode;
  }

  get axisType() {
    return this._value.axisType;
  }

  get opacity() {
    return this.barmode === 'overlay' ? 0.75 : 1.0;
  }

  get axisConfig() {
    return this._value.axisConfig;
  }

  toBuilder() {
    const { barmode, axisType, axisConfig } = this._value;

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder({ barmode, axisType, axisConfig });
  }

  static create(barmode: BarMode, axisType: AxisType = DEFAULT_AXIS_TYPE, axisConfig = DEFAULT_AXIS_CONFIG) {
    return new BarVisualizationConfig(barmode, axisType, axisConfig);
  }

  static empty() {
    return BarVisualizationConfig.create(DEFAULT_BARMODE);
  }

  toJSON() {
    const { barmode, axisType, axisConfig } = this._value;

    return {
      barmode,
      axis_type: axisType,
      axis_config: axisConfig,
    };
  }

  static fromJSON(_type: string, value: BarVisualizationConfigJson) {
    const { barmode, axis_type, axis_config } = value;

    return BarVisualizationConfig.create(barmode, axis_type, axis_config);
  }
}

type InternalBuilderState = {
  barmode: BarMode;
  axisType: AxisType;
  axisConfig: ChartAxisConfig;
};

class Builder {
  private readonly value: InternalBuilderState;

  constructor(value: InternalBuilderState) {
    this.value = Object.freeze({ ...value });
  }

  barmode(value: BarMode) {
    return new Builder({ ...this.value, barmode: value });
  }

  axisType(value: AxisType) {
    return new Builder({ ...this.value, axisType: value });
  }

  axisConfig(value: ChartAxisConfig) {
    return new Builder({ ...this.value, axisConfig: value });
  }

  build() {
    const { barmode, axisType } = this.value;

    return new BarVisualizationConfig(barmode, axisType);
  }
}
