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

import {
  arrayOfMaps,
  listWithObject,
  mapWithObject,
  mixedMapsAndObjects,
  objectWithMap,
  AlwaysEqual,
  NeverEqual,
  NonValueClass,
} from 'stores/__tests__/EqualityCheck.fixtures';

import isEqualForSearch from './isEqualForSearch';

export class AlwaysEqualForSearch {
  equalsForSearch = () => {
    return true;
  };
}

export class NeverEqualForSearch {
  equalsForSearch = () => {
    return false;
  };
}

describe('isEqualForSearch', () => {
  const verifyIsEqualForSearch = ({ initial, next, result }) => expect(isEqualForSearch(initial, next)).toBe(result);

  it.each`
    initial                  | next                     | result    | description
    ${undefined}             | ${undefined}             | ${true}   | ${'equal undefined values'}
    ${undefined}             | ${null}                  | ${false}  | ${'from undefined to null value'}
    ${undefined}             | ${42}                    | ${false}  | ${'from undefined to numeric value'}
    ${42}                    | ${42}                    | ${true}   | ${'equal numeric values'}
    ${42}                    | ${23}                    | ${false}  | ${'non-equal numeric values'}
    ${'Hello there!'}        | ${'Hello there!'}        | ${true}   | ${'equal string values'}
    ${'Hello there!'}        | ${'Hello World!'}        | ${false}  | ${'non-equal string values'}
    ${{}}                    | ${{}}                    | ${true}   | ${'equal empty objects'}
    ${{ bar: 23 }}           | ${{ bar: 23 }}           | ${true}  | ${'equal objects'}
    ${{ bar: 23 }}           | ${{ bar: 42 }}           | ${false}  | ${'non-equal objects'}
    ${[]}                    | ${[]}                    | ${true}   | ${'equal empty arrays'}
    ${[23]}                  | ${[23]}                  | ${true}   | ${'equal arrays'}
    ${[23]}                  | ${[42]}                  | ${false}  | ${'non-equal arrays'}
    ${Map()}                 | ${Map()}                 | ${true}   | ${'equal empty immutable maps'}
    ${Map({ bar: 23 })}      | ${Map({ bar: 23 })}      | ${true}   | ${'equal immutable maps'}
    ${Map({ bar: 23 })}      | ${Map({ bar: 42 })}      | ${false}  | ${'non-equal immutable maps'}
    ${List()}                | ${List()}                | ${true}   | ${'equal empty immutable lists'}
    ${List([23])}            | ${List([23])}            | ${true}   | ${'equal immutable lists'}
    ${List([23])}            | ${List([42])}            | ${false}  | ${'non-equal immutable lists'}
    ${new AlwaysEqual()}     | ${new AlwaysEqual()}     | ${true}   | ${'value class which is always equal'}
    ${new NeverEqual()}      | ${new NeverEqual()}      | ${false}  | ${'value class which is never equal'}
    ${new AlwaysEqual()}     | ${new NeverEqual()}      | ${true}   | ${'value class which is always equal'}
    ${new NeverEqual()}      | ${new AlwaysEqual()}     | ${false}  | ${'value class which is never equal'}
    ${new AlwaysEqualForSearch()} | ${new AlwaysEqualForSearch()} | ${true}   | ${'value class which is always equal'}
    ${new NeverEqualForSearch()}  | ${new NeverEqualForSearch()}  | ${false}  | ${'value class which is never equal'}
    ${new AlwaysEqualForSearch()} | ${new NeverEqualForSearch()}  | ${true}   | ${'value class which is always equal'}
    ${new NeverEqualForSearch()}  | ${new AlwaysEqualForSearch()} | ${false}  | ${'value class which is never equal'}
    ${new NonValueClass(23)} | ${new NonValueClass(42)} | ${false}   | ${'value class which is never equal'}
    ${mapWithObject()}       | ${mapWithObject()}       | ${true}   | ${'immutable maps containing objects'}
    ${listWithObject()}      | ${listWithObject()}      | ${true}   | ${'immutable lists containing objects'}
    ${objectWithMap()}       | ${objectWithMap()}       | ${true}   | ${'objects containing immutable maps'}
    ${arrayOfMaps()}         | ${arrayOfMaps()}         | ${true}   | ${'arrays containing immutable maps'}
    ${mixedMapsAndObjects()} | ${mixedMapsAndObjects()} | ${true}   | ${'nested immutable maps and objects'}
  `('compares $description and returns $result', verifyIsEqualForSearch);
});
