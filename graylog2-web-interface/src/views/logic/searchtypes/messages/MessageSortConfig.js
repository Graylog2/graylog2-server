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
import Direction from 'views/logic/aggregationbuilder/Direction';

type MessageSortConfigJson = {
  field: string,
  order: 'ASC' | 'DESC',
};

type InternalState = {
  field: string,
  direction: Direction,
};

export default class MessageSortConfig {
  _value: InternalState;

  constructor(field: string, direction: Direction) {
    this._value = { field, direction };
  }

  toJSON(): MessageSortConfigJson {
    const { field, direction } = this._value;

    return {
      field,
      order: direction === Direction.Ascending ? 'ASC' : 'DESC',
    };
  }

  static fromJSON({ field, order }: MessageSortConfigJson) {
    const direction = Direction.fromJSON(order === 'ASC' ? 'Ascending' : 'Descending');

    return new MessageSortConfig(field, direction);
  }
}
