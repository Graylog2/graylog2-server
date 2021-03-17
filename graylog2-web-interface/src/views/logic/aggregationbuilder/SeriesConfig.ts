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

export type SeriesConfigJson = {
  name: string,
};

type InternalState = {
  name: string | undefined | null,
};

export default class SeriesConfig {
  private readonly _value: InternalState;

  constructor(name: string | undefined | null) {
    this._value = { name };
  }

  get name() {
    return this._value.name;
  }

  toJSON() {
    const { name } = this._value;

    return { name };
  }

  static fromJSON(value: SeriesConfigJson) {
    const { name } = value;

    return new SeriesConfig(name);
  }

  static empty() {
    return new SeriesConfig(null);
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

  build() {
    const { name } = this.value.toObject();

    return new SeriesConfig(name);
  }
}
