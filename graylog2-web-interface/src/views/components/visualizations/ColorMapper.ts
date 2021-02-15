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
import { Map } from 'immutable';

import { defaultChartColors } from 'views/components/visualizations/Colors';

class ColorMapper {
  private _value: Map<string, string>;

  private _currentDefaultColor: number;

  constructor(colorMap = Map<string, string>(), colorIndex = -1) {
    this._value = colorMap;
    this._currentDefaultColor = colorIndex;
  }

  private _incrementColor() {
    this._currentDefaultColor = (this._currentDefaultColor + 1) % defaultChartColors.length;
  }

  private _nextFreeColor() {
    this._incrementColor();

    return defaultChartColors[this._currentDefaultColor];
  }

  get(name) {
    const color = this._value.get(name);

    if (color) {
      this._incrementColor();

      return color;
    }

    const newColor = this._nextFreeColor();
    this._value = this._value.set(name, newColor);

    return newColor;
  }

  set(name, color) {
    this._value = this._value.set(name, color);
  }

  toBuilder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(this._value, this._currentDefaultColor);
  }

  static builder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder();
  }

  static create(value = Map<string, string>()) {
    return new ColorMapper(value);
  }
}

class Builder {
  private value: Map<string, string>;

  private colorIndex: number;

  constructor(value = Map<string, string>(), colorIndex = -1) {
    this.value = value;
    this.colorIndex = colorIndex;
  }

  set(name, color) {
    return new Builder(this.value.set(name, color));
  }

  build() {
    return new ColorMapper(this.value, this.colorIndex);
  }
}

export default ColorMapper;
