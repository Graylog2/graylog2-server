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

import VisualizationConfig from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';
import { COLORSCALES } from 'views/logic/aggregationbuilder/visualizations/HeatmapVisualizationConfig';

export { COLORSCALES };

type ColorScale = (typeof COLORSCALES)[number];

type InternalState = {
  colorScale: ColorScale;
  reverseScale: boolean;
};

export type NetworkVisualizationConfigJSON = {
  color_scale: ColorScale;
  reverse_scale: boolean;
};

export default class NetworkVisualizationConfig extends VisualizationConfig {
  private readonly _value: InternalState;

  constructor(colorScale: InternalState['colorScale'], reverseScale: InternalState['reverseScale']) {
    super();

    this._value = { colorScale, reverseScale };
  }

  get colorScale() {
    return this._value.colorScale;
  }

  get reverseScale() {
    return this._value.reverseScale;
  }

  toBuilder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }

  static create(colorScale: InternalState['colorScale'], reverseScale: InternalState['reverseScale']) {
    return new NetworkVisualizationConfig(colorScale, reverseScale);
  }

  static empty() {
    return new NetworkVisualizationConfig('YlOrRd', false);
  }

  toJSON(): NetworkVisualizationConfigJSON {
    const { colorScale: color_scale, reverseScale: reverse_scale } = this._value;

    return { color_scale, reverse_scale };
  }

  static fromJSON(
    _type: string,
    value: NetworkVisualizationConfigJSON = { color_scale: 'YlOrRd', reverse_scale: false },
  ) {
    const { color_scale: colorScale, reverse_scale: reverseScale } = value;

    return NetworkVisualizationConfig.create(colorScale, reverseScale);
  }
}

type BuilderState = Immutable.Map<string, any>;

export class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  colorScale(value: InternalState['colorScale']) {
    return new Builder(this.value.set('colorScale', value));
  }

  reverseScale(value: InternalState['reverseScale']) {
    return new Builder(this.value.set('reverseScale', value));
  }

  build() {
    const { colorScale, reverseScale } = this.value.toObject();

    return new NetworkVisualizationConfig(colorScale, reverseScale);
  }
}
