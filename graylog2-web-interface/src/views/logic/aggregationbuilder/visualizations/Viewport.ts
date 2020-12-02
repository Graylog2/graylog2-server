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

type Center = [number, number];
type Zoom = number;
type State = {
  center: Center,
  zoom: Zoom,
};

export type ViewportJson = {
  center_x: number,
  center_y: number,
  zoom: number,
};

export default class Viewport {
  _value: State;

  constructor(center: Center, zoom: Zoom) {
    this._value = { center, zoom };
  }

  get center() {
    return this._value.center;
  }

  get zoom() {
    return this._value.zoom;
  }

  toBuilder() {
    const { center, zoom } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ center, zoom }));
  }

  static create(center: Center, zoom: Zoom) {
    return new Viewport(center, zoom);
  }

  toJSON(): ViewportJson {
    const { center, zoom } = this._value;

    return {
      center_x: center[0],
      center_y: center[1],
      zoom,
    };
  }

  static fromJSON(value: ViewportJson) {
    // eslint-disable-next-line camelcase
    const { center_x, center_y, zoom } = value;

    // eslint-disable-next-line camelcase
    return Viewport.create([center_x, center_y], zoom);
  }
}

type InternalBuilderState = Immutable.Map<string, any>;

class Builder {
  value: InternalBuilderState;

  constructor(value: InternalBuilderState = Immutable.Map()) {
    this.value = value;
  }

  center(value: Center) {
    return new Builder(this.value.set('center', value));
  }

  zoom(value: Zoom) {
    return new Builder(this.value.set('zoom', value));
  }

  build() {
    const { center, zoom } = this.value.toObject();

    return new Viewport(center, zoom);
  }
}
