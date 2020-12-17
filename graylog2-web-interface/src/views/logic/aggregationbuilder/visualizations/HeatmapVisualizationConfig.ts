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
  autoScale: boolean,
  zMin: number | undefined | null,
  zMid: number | undefined | null,
  zMax: number | undefined | null,
  defaultValue: number | undefined | null,
};

export type HeatmapVisualizationConfigJSON = {
  color_scale: ColorScale;
  reverse_scale: boolean;
  auto_scale: boolean,
  z_min: number | undefined | null,
  z_mid: number | undefined | null,
  z_max: number | undefined | null,
  default_value: number | undefined | null,
}

export default class HeatmapVisualizationConfig extends VisualizationConfig {
  private readonly _value: InternalState;

  constructor(
    colorScale: InternalState['colorScale'],
    reverseScale: InternalState['reverseScale'],
    autoScale: InternalState['autoScale'],
    zMin: InternalState['zMin'],
    zMid: InternalState['zMid'],
    zMax: InternalState['zMax'],
    defaultValue: InternalState['defaultValue'],
  ) {
    super();

    this._value = {
      colorScale,
      reverseScale,
      autoScale,
      zMax,
      zMid,
      zMin,
      defaultValue,
    };
  }

  get colorScale() {
    return this._value.colorScale;
  }

  get reverseScale() {
    return this._value.reverseScale;
  }

  get autoScale() {
    return this._value.autoScale;
  }

  get zMin() {
    return this._value.zMin;
  }

  get zMid() {
    return this._value.zMid;
  }

  get zMax() {
    return this._value.zMax;
  }

  get defaultValue() {
    return this._value.defaultValue;
  }

  toBuilder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }

  static create(
    colorScale: InternalState['colorScale'],
    reverseScale: InternalState['reverseScale'],
    autoScale: InternalState['autoScale'],
    zMin: InternalState['zMin'],
    zMid: InternalState['zMid'],
    zMax: InternalState['zMax'],
    defaultValue: InternalState['defaultValue'],
  ) {
    return new HeatmapVisualizationConfig(
      colorScale,
      reverseScale,
      autoScale,
      zMin,
      zMid,
      zMax,
      defaultValue,
    );
  }

  static empty() {
    return new HeatmapVisualizationConfig(
      'Viridis',
      false,
      true,
      undefined,
      undefined,
      undefined,
      undefined,
    );
  }

  toJSON(): HeatmapVisualizationConfigJSON {
    const {
      colorScale: color_scale,
      reverseScale: reverse_scale,
      autoScale: auto_scale,
      zMin: z_min,
      zMid: z_mid,
      zMax: z_max,
      defaultValue: default_value,
    } = this._value;

    return {
      color_scale,
      reverse_scale,
      auto_scale,
      z_min,
      z_mid,
      z_max,
      default_value,
    };
  }

  static fromJSON(type: string, value: HeatmapVisualizationConfigJSON = {
    color_scale: 'Viridis',
    reverse_scale: false,
    auto_scale: true,
    z_min: undefined,
    z_mid: undefined,
    z_max: undefined,
    default_value: undefined,
  }) {
    const {
      color_scale: colorScale,
      reverse_scale: reverseScale,
      auto_scale: autoScale,
      z_min: zMin,
      z_mid: zMid,
      z_max: zMax,
      default_value: defaultValue,
    } = value;

    return HeatmapVisualizationConfig.create(colorScale, reverseScale, autoScale, zMin, zMid, zMax, defaultValue);
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

  autoScale(value: InternalState['autoScale']) {
    return new Builder(this.value.set('autoScale', value));
  }

  zMin(value: InternalState['zMin']) {
    return new Builder(this.value.set('zMin', value));
  }

  zMid(value: InternalState['zMid']) {
    return new Builder(this.value.set('zMid', value));
  }

  zMax(value: InternalState['zMax']) {
    return new Builder(this.value.set('zMax', value));
  }

  defaultValue(value: InternalState['defaultValue']) {
    return new Builder(this.value.set('defaultValue', value));
  }

  build() {
    const {
      colorScale,
      reverseScale,
      autoScale,
      zMin,
      zMid,
      zMax,
      defaultValue,
    } = this.value.toObject();

    return new HeatmapVisualizationConfig(colorScale, reverseScale, autoScale, zMin, zMid, zMax, defaultValue);
  }
}
