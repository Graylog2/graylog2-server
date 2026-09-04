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
import * as Immutable from 'immutable';

import { DEFAULT_INTERPOLATION } from 'views/Constants';
import type {
  XYVisualization,
  AxisType,
  ChartAxisConfig,
} from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import { DEFAULT_AXIS_CONFIG, DEFAULT_AXIS_TYPE } from 'views/logic/aggregationbuilder/visualizations/XYVisualization';

import VisualizationConfig from './VisualizationConfig';
import type { InterpolationMode } from './Interpolation';

type InternalState = {
  interpolation: InterpolationMode;
  axisType: AxisType;
  axisConfig: ChartAxisConfig;
};

export type LineVisualizationConfigJSON = {
  interpolation: InterpolationMode;
  axis_type?: AxisType;
  axis_config?: ChartAxisConfig;
};

export default class LineVisualizationConfig extends VisualizationConfig implements XYVisualization {
  private readonly _value: InternalState;

  constructor(
    interpolation: InternalState['interpolation'],
    axisType: InternalState['axisType'] = DEFAULT_AXIS_TYPE,
    axisConfig: ChartAxisConfig = DEFAULT_AXIS_CONFIG,
  ) {
    super();
    this._value = { interpolation, axisType, axisConfig };
  }

  get interpolation() {
    return this._value.interpolation;
  }

  get axisType() {
    return this._value.axisType;
  }

  get axisConfig() {
    return this._value.axisConfig;
  }

  toBuilder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }

  static create(
    interpolation: InternalState['interpolation'],
    axisType: InternalState['axisType'] = DEFAULT_AXIS_TYPE,
    axisConfig: InternalState['axisConfig'] = DEFAULT_AXIS_CONFIG,
  ) {
    return new LineVisualizationConfig(interpolation, axisType, axisConfig);
  }

  static empty() {
    return new LineVisualizationConfig(DEFAULT_INTERPOLATION, DEFAULT_AXIS_TYPE, DEFAULT_AXIS_CONFIG);
  }

  toJSON() {
    const { interpolation, axisType, axisConfig } = this._value;

    return {
      interpolation,
      axis_type: axisType,
      axis_config: axisConfig,
    };
  }

  static fromJSON(_type: string, value: LineVisualizationConfigJSON) {
    return LineVisualizationConfig.create(
      value?.interpolation ?? DEFAULT_INTERPOLATION,
      value?.axis_type ?? DEFAULT_AXIS_TYPE,
      value?.axis_config ?? DEFAULT_AXIS_CONFIG,
    );
  }
}

type BuilderState = Immutable.Map<string, any>;

class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  interpolation(value: InternalState['interpolation']) {
    return new Builder(this.value.set('interpolation', value));
  }

  axisType(value: InternalState['axisType']) {
    return new Builder(this.value.set('axisType', value));
  }

  axisConfig(value: InternalState['axisConfig']) {
    return new Builder(this.value.set('axisConfig', value));
  }

  build() {
    const { interpolation, axisType, axisConfig } = this.value.toObject();

    return new LineVisualizationConfig(interpolation, axisType, axisConfig);
  }
}
