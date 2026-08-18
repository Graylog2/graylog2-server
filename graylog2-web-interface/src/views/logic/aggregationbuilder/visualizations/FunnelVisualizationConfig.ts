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
import VisualizationConfig from './VisualizationConfig';

export const DEFAULT_FUNNEL_START_COLOR = '#1F77B4';
export const DEFAULT_FUNNEL_END_COLOR = '#AEC7E8';

type FunnelVisualizationConfigJson = {
  start_color: string;
  end_color: string;
};

export default class FunnelVisualizationConfig extends VisualizationConfig {
  private readonly _value: { startColor: string; endColor: string };

  constructor(startColor: string, endColor: string) {
    super();
    this._value = { startColor, endColor };
  }

  get startColor() {
    return this._value.startColor;
  }

  get endColor() {
    return this._value.endColor;
  }

  static create(startColor: string, endColor: string) {
    return new FunnelVisualizationConfig(startColor, endColor);
  }

  static empty() {
    return FunnelVisualizationConfig.create(DEFAULT_FUNNEL_START_COLOR, DEFAULT_FUNNEL_END_COLOR);
  }

  toJSON(): FunnelVisualizationConfigJson {
    return {
      start_color: this._value.startColor,
      end_color: this._value.endColor,
    };
  }

  static fromJSON(_type: string, value: FunnelVisualizationConfigJson) {
    return FunnelVisualizationConfig.create(value.start_color, value.end_color);
  }
}
