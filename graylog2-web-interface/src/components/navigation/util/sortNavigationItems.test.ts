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
import sortNavigationItems from './sortNavigationItems';

type Item = { description: string; position?: { after?: string; last?: boolean } };

const item = (description: string, position?: Item['position']): Item => ({ description, position });
const descriptions = (items: Array<Item>) => items.map(({ description }) => description);

describe('sortNavigationItems', () => {
  it('keeps the order of items without a position', () => {
    expect(descriptions(sortNavigationItems([item('A'), item('B'), item('C')]))).toEqual(['A', 'B', 'C']);
  });

  it('places an item after its anchor', () => {
    expect(descriptions(sortNavigationItems([item('X', { after: 'A' }), item('A'), item('B')]))).toEqual([
      'A',
      'X',
      'B',
    ]);
  });

  it('keeps the registration order of items positioned after the same item', () => {
    expect(
      descriptions(sortNavigationItems([item('A'), item('B'), item('X', { after: 'A' }), item('Y', { after: 'A' })])),
    ).toEqual(['A', 'X', 'Y', 'B']);
  });

  it('places an item after an anchor that is registered after it', () => {
    expect(
      descriptions(sortNavigationItems([item('A'), item('B'), item('Y', { after: 'X' }), item('X', { after: 'A' })])),
    ).toEqual(['A', 'X', 'Y', 'B']);
  });

  it('keeps chained items together when another item has the same anchor', () => {
    expect(
      descriptions(
        sortNavigationItems([
          item('A'),
          item('B'),
          item('X', { after: 'A' }),
          item('Y', { after: 'X' }),
          item('Z', { after: 'A' }),
        ]),
      ),
    ).toEqual(['A', 'X', 'Y', 'Z', 'B']);
  });

  it('appends items with a missing anchor before items positioned last', () => {
    expect(
      descriptions(sortNavigationItems([item('L', { last: true }), item('A'), item('X', { after: 'Missing' })])),
    ).toEqual(['A', 'X', 'L']);
  });

  it.each([
    [[item('A'), item('B'), item('X', { after: 'A' }), item('Y', { after: 'A' })]],
    [[item('A'), item('B'), item('Y', { after: 'X' }), item('X', { after: 'A' }), item('Z', { after: 'A' })]],
    [[item('L', { last: true }), item('A'), item('X', { after: 'Missing' }), item('Y', { after: 'A' })]],
  ])('returns the same order when sorting an already sorted list', (items) => {
    const sorted = sortNavigationItems(items);

    expect(descriptions(sortNavigationItems(sorted))).toEqual(descriptions(sorted));
  });
});
