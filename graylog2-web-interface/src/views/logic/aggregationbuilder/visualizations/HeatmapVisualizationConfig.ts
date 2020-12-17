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
};

export type HeatmapVisualizationConfigJSON = {
  color_scale: ColorScale;
}

export default class HeatmapVisualizationConfig extends VisualizationConfig {
  private readonly _value: InternalState;

  constructor(colorScale: InternalState['colorScale']) {
    super();

    this._value = { colorScale };
  }

  get colorScale() {
    return this._value.colorScale;
  }

  toBuilder() {
    return new Builder(Immutable.Map(this._value));
  }

  static create(colorScale: InternalState['colorScale']) {
    return new HeatmapVisualizationConfig(colorScale);
  }

  static empty() {
    return new HeatmapVisualizationConfig('Viridis');
  }

  toJSON() {
    const { colorScale } = this._value;

    return { colorScale };
  }

  static fromJSON(type: string, value: HeatmapVisualizationConfigJSON = { color_scale: 'Viridis' }) {
    const { color_scale: colorScale } = value;

    return HeatmapVisualizationConfig.create(colorScale);
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

  build() {
    const { colorScale } = this.value.toObject();

    return new HeatmapVisualizationConfig(colorScale);
  }
}
