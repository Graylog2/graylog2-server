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
import { $PropertyType } from 'utility-types';

import VisualizationConfig from './VisualizationConfig';
import type { InterpolationMode } from './Interpolation';

type InternalState = {
  interpolation: InterpolationMode,
};

export type LineVisualizationConfigJSON = {
  interpolation: InterpolationMode,
};

export default class LineVisualizationConfig extends VisualizationConfig {
  private readonly _value: InternalState;

  static readonly DEFAULT_INTERPOLATION: InterpolationMode = 'linear';

  constructor(interpolation: $PropertyType<InternalState, 'interpolation'>) {
    super();
    this._value = { interpolation };
  }

  get interpolation() {
    return this._value.interpolation;
  }

  toBuilder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }

  static create(interpolation: $PropertyType<InternalState, 'interpolation'>) {
    return new LineVisualizationConfig(interpolation);
  }

  static empty() {
    return new LineVisualizationConfig(this.DEFAULT_INTERPOLATION);
  }

  toJSON() {
    const { interpolation } = this._value;

    return { interpolation };
  }

  static fromJSON(type: string, value: LineVisualizationConfigJSON) {
    return LineVisualizationConfig.create(value?.interpolation ?? this.DEFAULT_INTERPOLATION);
  }
}

type BuilderState = Immutable.Map<string, any>;

class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  interpolation(value: $PropertyType<InternalState, 'interpolation'>) {
    return new Builder(this.value.set('interpolation', value));
  }

  build() {
    const { interpolation } = this.value.toObject();

    return new LineVisualizationConfig(interpolation);
  }
}
