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
import Viewport from './Viewport';
import type { ViewportJson } from './Viewport';

type State = {
  viewport: Viewport,
};

type WorldMapVisualizationConfigJson = {
  viewport: ViewportJson,
};

export default class WorldMapVisualizationConfig extends VisualizationConfig {
  _value: State;

  constructor(viewport: Viewport) {
    super();
    this._value = { viewport };
  }

  get viewport() {
    return this._value.viewport;
  }

  toBuilder() {
    const { viewport } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ viewport }));
  }

  static create(viewport: Viewport) {
    return new WorldMapVisualizationConfig(viewport);
  }

  toJSON() {
    const { viewport } = this._value;

    return {
      viewport,
    };
  }

  static fromJSON(type: string, value: WorldMapVisualizationConfigJson) {
    const { viewport } = value;

    return WorldMapVisualizationConfig.builder()
      .viewport(Viewport.fromJSON(viewport))
      .build();
  }

  // eslint-disable-next-line no-use-before-define
  static builder(): Builder {
    // eslint-disable-next-line no-use-before-define
    return new Builder();
  }
}

type InternalBuilderState = Immutable.Map<string, any>;
class Builder {
  value: InternalBuilderState;

  constructor(value: InternalBuilderState = Immutable.Map()) {
    this.value = value;
  }

  viewport(value: Viewport) {
    return new Builder(this.value.set('viewport', value));
  }

  build() {
    const { viewport } = this.value.toObject();

    return new WorldMapVisualizationConfig(viewport);
  }
}
