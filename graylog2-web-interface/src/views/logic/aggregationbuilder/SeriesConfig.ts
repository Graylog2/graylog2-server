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

export type SeriesThreshold = {
  name: string;
  color: string;
  value: number;
};

export type SeriesConfigJson = {
  name: string;
  thresholds: Array<SeriesThreshold>;
};

type InternalState = {
  name: string | undefined | null;
  thresholds?: Array<SeriesThreshold> | undefined | null;
};

export default class SeriesConfig {
  private readonly _value: InternalState;

  constructor(name: string | undefined | null, thresholds: Array<SeriesThreshold> | undefined | null) {
    this._value = { name, thresholds };
  }

  get name() {
    return this._value.name;
  }

  get thresholds() {
    return this._value.thresholds;
  }

  toJSON() {
    const { name, thresholds } = this._value;

    return { name, thresholds };
  }

  static fromJSON(value: SeriesConfigJson) {
    const { name, thresholds } = value;

    return new SeriesConfig(name, thresholds);
  }

  static empty() {
    return new SeriesConfig(null, null);
  }

  toBuilder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }
}

type BuilderState = Immutable.Map<string, any>;

class Builder {
  private readonly value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  name(newName: string) {
    return new Builder(this.value.set('name', newName));
  }

  thresholds(thresholds: Array<SeriesThreshold>) {
    return new Builder(this.value.set('thresholds', thresholds));
  }

  build() {
    const { name, thresholds } = this.value.toObject();

    return new SeriesConfig(name, thresholds);
  }
}
