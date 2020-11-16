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
export type DirectionJson = 'Ascending' | 'Descending';

export default class Direction {
  static Ascending = new Direction('Ascending');

  static Descending = new Direction('Descending');

  _direction: DirectionJson;

  constructor(direction: DirectionJson) {
    this._direction = direction;
  }

  toJSON(): DirectionJson {
    return this._direction;
  }

  get direction() {
    return this._direction;
  }

  equals(other: any) {
    return other && other.direction === this._direction;
  }

  static fromJSON(value: DirectionJson): Direction {
    return Direction.fromString(value);
  }

  static fromString(value: string): Direction {
    switch (value) {
      case 'Ascending': return Direction.Ascending;
      case 'Descending': return Direction.Descending;
      default: throw new Error(`Invalid direction: ${value}`);
    }
  }
}
