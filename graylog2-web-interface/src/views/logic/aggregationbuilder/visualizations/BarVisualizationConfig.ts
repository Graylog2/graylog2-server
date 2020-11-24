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

import VisualizationConfig from './VisualizationConfig';

export type BarMode = 'stack' | 'group' | 'overlay' | 'relative';

export type BarVisualizationConfigType = {
  barmode: BarMode,
};

export default class BarVisualizationConfig extends VisualizationConfig {
  _value: BarVisualizationConfigType;

  constructor(barmode: BarMode) {
    super();
    this._value = { barmode };
  }

  get barmode() {
    return this._value.barmode;
  }

  get opacity() {
    return this.barmode === 'overlay' ? 0.75 : 1.0;
  }

  toBuilder() {
    const { barmode } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ barmode }));
  }

  static create(barmode: BarMode) {
    return new BarVisualizationConfig(barmode);
  }

  toJSON() {
    const { barmode } = this._value;

    return {
      barmode,
    };
  }

  static fromJSON(type: string, value: BarVisualizationConfigType) {
    const { barmode } = value;

    return BarVisualizationConfig.create(barmode);
  }
}

type InternalBuilderState = Immutable.Map<string, any>;

class Builder {
  value: InternalBuilderState;

  constructor(value: InternalBuilderState = Immutable.Map()) {
    this.value = value;
  }

  barmode(value: BarMode) {
    return new Builder(this.value.set('barmode', value));
  }

  build() {
    const { barmode } = this.value.toObject();

    return new BarVisualizationConfig(barmode);
  }
}
