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
import { hashKey } from '@tanstack/react-query';

import hashQueryKey from './hashQueryKey';

describe('hashQueryKey', () => {
  it('hashes keys containing BigInt values', () => {
    expect(hashQueryKey(['message', { big_number: BigInt('9223372036854775807') }])).toBe(
      '["message",{"big_number":9223372036854775807}]',
    );
  });

  it('distinguishes BigInt values from strings with the same digits', () => {
    expect(hashQueryKey(['message', BigInt('9223372036854775807')])).not.toBe(
      hashQueryKey(['message', '9223372036854775807']),
    );
  });

  it('ignores the order of object keys', () => {
    expect(hashQueryKey(['stream', { b: BigInt('9223372036854775807'), a: 1 }])).toBe(
      hashQueryKey(['stream', { a: 1, b: BigInt('9223372036854775807') }]),
    );
  });

  it.each([
    [['streams']],
    [['stream', 'stream-id', { page: 1, perPage: 10, query: 'foo' }]],
    [['search', { filters: [{ field: 'source', values: ['a', 'b'] }], timerange: { type: 'relative', range: 300 } }]],
    [['nested', { z: { y: 1, x: [null, true, 'value'] }, a: undefined }]],
  ])('creates the same hash as react-query for keys without BigInt values: %j', (queryKey) => {
    expect(hashQueryKey(queryKey)).toBe(hashKey(queryKey));
  });
});
