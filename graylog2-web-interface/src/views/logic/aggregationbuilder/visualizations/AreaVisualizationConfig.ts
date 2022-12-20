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

import type { AxisType, XYVisualization } from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import { DEFAULT_AXIS_TYPE } from 'views/logic/aggregationbuilder/visualizations/XYVisualization';
import { DEFAULT_INTERPOLATION } from 'views/Constants';

import VisualizationConfig from './VisualizationConfig';
import type { InterpolationMode } from './Interpolation';

type InternalState = {
  interpolation: InterpolationMode,
  axisType: AxisType,
};

export type AreaVisualizationConfigJSON = {
  interpolation: InterpolationMode,
  axis_type?: AxisType,
};

export default class AreaVisualizationConfig extends VisualizationConfig implements XYVisualization {
  private readonly _value: InternalState;

  constructor(interpolation: InternalState['interpolation'], axisType: InternalState['axisType'] = DEFAULT_AXIS_TYPE) {
    super();
    this._value = { interpolation, axisType };
  }

  get interpolation() {
    return this._value.interpolation;
  }

  get axisType() {
    return this._value.axisType;
  }

  toBuilder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }

  static create(interpolation: InternalState['interpolation'], axisType: InternalState['axisType'] = DEFAULT_AXIS_TYPE) {
    return new AreaVisualizationConfig(interpolation, axisType);
  }

  static empty() {
    return AreaVisualizationConfig.create(DEFAULT_INTERPOLATION);
  }

  toJSON() {
    const { interpolation, axisType } = this._value;

    return {
      interpolation,
      axis_type: axisType,
    };
  }

  static fromJSON(_type: string, value: AreaVisualizationConfigJSON) {
    return AreaVisualizationConfig.create(
      value?.interpolation ?? DEFAULT_INTERPOLATION,
      value?.axis_type ?? DEFAULT_AXIS_TYPE,
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

  build() {
    const { interpolation, axisType } = this.value.toObject();

    return new AreaVisualizationConfig(interpolation, axisType);
  }
}
