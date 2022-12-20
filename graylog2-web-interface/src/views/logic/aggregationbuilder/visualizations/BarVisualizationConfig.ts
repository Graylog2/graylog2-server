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
import type { XYVisualization, AxisType } from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import { DEFAULT_AXIS_TYPE } from 'views/logic/aggregationbuilder/visualizations/XYVisualization';

import VisualizationConfig from './VisualizationConfig';

export const DEFAULT_BARMODE = 'group';

export type BarMode = 'stack' | 'group' | 'overlay' | 'relative';

export type BarVisualizationConfigType = {
  barmode: BarMode,
  axisType: AxisType,
};

export type BarVisualizationConfigJson = {
  barmode: BarMode,
  axis_type: AxisType,
};

export default class BarVisualizationConfig extends VisualizationConfig implements XYVisualization {
  _value: BarVisualizationConfigType;

  constructor(barmode: BarMode, axisType: AxisType) {
    super();
    this._value = { barmode, axisType };
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

  toBuilder() {
    const { barmode, axisType } = this._value;

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder({ barmode, axisType });
  }

  static create(barmode: BarMode, axisType: AxisType = DEFAULT_AXIS_TYPE) {
    return new BarVisualizationConfig(barmode, axisType);
  }

  static empty() {
    return BarVisualizationConfig.create(DEFAULT_BARMODE);
  }

  toJSON() {
    const { barmode, axisType } = this._value;

    return {
      barmode,
      axis_type: axisType,
    };
  }

  static fromJSON(_type: string, value: BarVisualizationConfigJson) {
    const { barmode, axis_type } = value;

    return BarVisualizationConfig.create(barmode, axis_type);
  }
}

type InternalBuilderState = {
  barmode: BarMode,
  axisType: AxisType,
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

  build() {
    const { barmode, axisType } = this.value;

    return new BarVisualizationConfig(barmode, axisType);
  }
}
