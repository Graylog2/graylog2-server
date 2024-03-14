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
import Direction from 'views/logic/aggregationbuilder/Direction';

type EventsListSortConfigJson = {
  field: string,
  direction: 'ASC' | 'DESC',
};

type InternalState = {
  field: string,
  direction: Direction,
};

export default class EventsListSortConfig {
  _value: InternalState;

  constructor(field: string, direction: Direction) {
    this._value = { field, direction };
  }

  toJSON(): EventsListSortConfigJson {
    const { field, direction } = this._value;

    return {
      field,
      direction: direction === Direction.Ascending ? 'ASC' : 'DESC',
    };
  }

  static fromJSON({ field, direction }: EventsListSortConfigJson) {
    const directionJSON = Direction.fromJSON(direction === 'ASC' ? 'Ascending' : 'Descending');

    return new EventsListSortConfig(field, directionJSON);
  }
}
