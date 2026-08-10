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
import { eventsDisplayName } from 'views/logic/searchtypes/events/EventHandler';

class ColorMapper {
  private _value: Map<string, string>;

  private _currentDefaultColor: number;

  private _defaultColors: Array<string>;

  constructor(colorMap = Map<string, string>(), colorIndex = -1, defaultColors: Array<string> = defaultChartColors) {
    this._value = colorMap;
    this._currentDefaultColor = colorIndex;
    this._defaultColors = defaultColors?.length ? defaultColors : defaultChartColors;
  }

  private _incrementColor() {
    this._currentDefaultColor = (this._currentDefaultColor + 1) % this._defaultColors.length;
  }

  private _nextFreeColor() {
    this._incrementColor();

    return this._defaultColors[this._currentDefaultColor];
  }

  get(name, defaultColor?) {
    const color = this._value.get(name);

    if (color) {
      if (name !== eventsDisplayName) {
        this._incrementColor();
      }

      return color;
    }

    if (defaultColor) {
      if (name !== eventsDisplayName) {
        this._incrementColor();
      }

      this._value = this._value.set(name, defaultColor);

      return defaultColor;
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
    return new Builder(this._value, this._currentDefaultColor, this._defaultColors);
  }

  static builder(defaultColors?: Array<string>) {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(undefined, undefined, defaultColors);
  }

  static create(value = Map<string, string>()) {
    return new ColorMapper(value);
  }
}

class Builder {
  private value: Map<string, string>;

  private colorIndex: number;

  private defaultColors: Array<string>;

  constructor(value = Map<string, string>(), colorIndex = -1, defaultColors: Array<string> = defaultChartColors) {
    this.value = value;
    this.colorIndex = colorIndex;
    this.defaultColors = defaultColors;
  }

  set(name, color) {
    return new Builder(this.value.set(name, color), this.colorIndex, this.defaultColors);
  }

  build() {
    return new ColorMapper(this.value, this.colorIndex, this.defaultColors);
  }
}

export default ColorMapper;
