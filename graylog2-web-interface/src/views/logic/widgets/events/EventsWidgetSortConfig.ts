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

import type { DirectionJson } from 'views/logic/aggregationbuilder/Direction';
import Direction from 'views/logic/aggregationbuilder/Direction';

export type EventsWidgetSortConfigJSON = {
  field: string,
  direction: DirectionJson,
};

type InternalState = {
  field: string,
  direction: Direction,
};

export default class EventsWidgetSortConfig {
  private readonly _value: InternalState;

  constructor(field: string, direction: Direction) {
    this._value = { field, direction };
  }

  get field() {
    return this._value.field;
  }

  get direction() {
    return this._value.direction;
  }

  toJSON(): EventsWidgetSortConfigJSON {
    const { field, direction } = this._value;

    return {
      field,
      direction: direction as unknown as DirectionJson,
    };
  }

  static fromJSON(value: EventsWidgetSortConfigJSON) {
    const { field, direction } = value;

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder()
      .field(field)
      .direction(Direction.fromJSON(direction))
      .build();
  }

  toBuilder(): Builder {
    const { field, direction } = this._value;

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map({ field, direction }));
  }
}

type BuilderState = Immutable.Map<string, any>;
export class Builder {
  private readonly value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  field(value: string) {
    return new Builder(this.value.set('field', value));
  }

  direction(value: Direction) {
    return new Builder(this.value.set('direction', value));
  }

  build() {
    const { field, direction } = this.value.toObject();

    return new EventsWidgetSortConfig(field, direction);
  }
}
