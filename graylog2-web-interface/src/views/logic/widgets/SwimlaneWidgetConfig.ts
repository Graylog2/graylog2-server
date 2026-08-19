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

import WidgetConfig from './WidgetConfig';

export const DEFAULT_LIMIT = 500;
export const DEFAULT_MAX_LANES = 20;

type InternalState = {
  laneField: string;
  colorField: string | undefined;
  limit: number;
  maxLanes: number;
};

export type SwimlaneWidgetConfigJSON = {
  lane_field: string;
  color_field?: string;
  limit: number;
  max_lanes: number;
};

export default class SwimlaneWidgetConfig extends WidgetConfig {
  _value: InternalState;

  constructor(laneField: string, colorField: string | undefined, limit: number, maxLanes: number) {
    super();
    this._value = { laneField, colorField, limit, maxLanes };
  }

  get laneField() {
    return this._value.laneField;
  }

  get colorField() {
    return this._value.colorField;
  }

  get limit() {
    return this._value.limit;
  }

  get maxLanes() {
    return this._value.maxLanes;
  }

  toBuilder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }

  toJSON(): SwimlaneWidgetConfigJSON {
    const { laneField, colorField, limit, maxLanes } = this._value;

    return {
      lane_field: laneField,
      color_field: colorField,
      limit,
      max_lanes: maxLanes,
    };
  }

  static builder(): Builder {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder()
      .laneField('')
      .colorField(undefined)
      .limit(DEFAULT_LIMIT)
      .maxLanes(DEFAULT_MAX_LANES);
  }

  static fromJSON(value: SwimlaneWidgetConfigJSON): SwimlaneWidgetConfig {
    return new SwimlaneWidgetConfig(
      value.lane_field,
      value.color_field,
      value.limit ?? DEFAULT_LIMIT,
      value.max_lanes ?? DEFAULT_MAX_LANES,
    );
  }
}

type BuilderState = Immutable.Map<string, any>;

class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  laneField(v: string) {
    return new Builder(this.value.set('laneField', v));
  }

  colorField(v: string | undefined) {
    return new Builder(this.value.set('colorField', v));
  }

  limit(v: number) {
    return new Builder(this.value.set('limit', v));
  }

  maxLanes(v: number) {
    return new Builder(this.value.set('maxLanes', v));
  }

  build() {
    const { laneField, colorField, limit, maxLanes } = this.value.toObject();

    return new SwimlaneWidgetConfig(laneField, colorField, limit, maxLanes);
  }
}
