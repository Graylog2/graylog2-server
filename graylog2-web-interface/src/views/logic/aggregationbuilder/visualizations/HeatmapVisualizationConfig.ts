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

type ColorScale =
    'Greys'
  | 'YlGnBu'
  | 'Greens'
  | 'YlOrRd'
  | 'Bluered'
  | 'RdBu'
  | 'Reds'
  | 'Blues'
  | 'Picnic'
  | 'Rainbow'
  | 'Portland'
  | 'Jet'
  | 'Hot'
  | 'Blackbody'
  | 'Earth'
  | 'Electric'
  | 'Viridis'
  | 'Cividis';

export const COLORSCALES = ['Greys', 'YlGnBu', 'Greens', 'YlOrRd', 'Bluered', 'RdBu', 'Reds', 'Blues', 'Picnic', 'Rainbow', 'Portland', 'Jet', 'Hot', 'Blackbody', 'Earth', 'Electric', 'Viridis', 'Cividis'];

type InternalState = {
  colorScale: ColorScale;
  reverseScale: boolean;
};

export type HeatmapVisualizationConfigJSON = {
  color_scale: ColorScale;
  reverse_scale: boolean;
}

export default class HeatmapVisualizationConfig extends VisualizationConfig {
  private readonly _value: InternalState;

  constructor(colorScale: InternalState['colorScale'], reverseScale: InternalState['reverseScale']) {
    super();

    this._value = { colorScale, reverseScale };
  }

  get colorScale() {
    return this._value.colorScale;
  }

  get reverseScale () {
    return this._value.reverseScale;
  }

  toBuilder() {
    return new Builder(Immutable.Map(this._value));
  }

  static create(colorScale: InternalState['colorScale'], reverseScale: InternalState['reverseScale']) {
    return new HeatmapVisualizationConfig(colorScale, reverseScale);
  }

  static empty() {
    return new HeatmapVisualizationConfig('Viridis', false);
  }

  toJSON(): HeatmapVisualizationConfigJSON {
    const {
      colorScale: color_scale,
      reverseScale: reverse_scale,
    } = this._value;

    return { color_scale, reverse_scale };
  }

  static fromJSON(type: string, value: HeatmapVisualizationConfigJSON = { color_scale: 'Viridis', reverse_scale: false }) {
    const {
      color_scale: colorScale,
      reverse_scale: reverseScale,
    } = value;

    return HeatmapVisualizationConfig.create(colorScale, reverseScale);
  }
}

type BuilderState = Immutable.Map<string, any>;

class Builder {
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

    return new HeatmapVisualizationConfig(colorScale, reverseScale);
  }
}
