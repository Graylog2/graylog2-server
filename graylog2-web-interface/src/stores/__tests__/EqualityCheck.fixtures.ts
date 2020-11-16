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
import { List, Map } from 'immutable';

type MixedMapsAndObjects = { [string]: Map<string, { [string]: Map<string, number> }> };

export const mapWithObject = (): Map<string, { [string]: number }> => Map({ foo: { bar: 42 } });
export const listWithObject = (): List<{ [string]: { [string]: number } }> => List([{ foo: { bar: 42 } }]);
export const objectWithMap = (): { [string]: Map<string, number> } => ({ foo: Map({ bar: 42 }) });
export const arrayOfMaps = (): Array<Map<string, number>> => [Map({ foo: 23 }), Map({ bar: 42 })];
export const mixedMapsAndObjects = (): MixedMapsAndObjects => ({ foo: Map({ bar: { baz: Map({ qux: 42 }) } }) });

export class AlwaysEqual {
  equals = () => {
    return true;
  };
}

export class NeverEqual {
  equals = () => {
    return false;
  };
}

export class NonValueClass {
  value: number;

  constructor(value: number) {
    this.value = value;
  }
}
