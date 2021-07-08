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
import deduplicateValues from './DeduplicateValues';

describe('DeduplicateValues', () => {
  it('should not fail for empty rows', () => {
    expect(deduplicateValues([], [])).toEqual([]);
  });

  it('should deduplicate values', () => {
    const rows = [
      { controller: 'FooController', action: 'index' },
      { controller: 'FooController', action: 'create' },
      { controller: 'FooController', action: 'update' },
      { controller: 'FooController', action: 'delete' },
    ];
    const result = deduplicateValues(rows, ['controller', 'action']);

    expect(result).toEqual([
      { controller: 'FooController', action: 'index' },
      { action: 'create' },
      { action: 'update' },
      { action: 'delete' },
    ]);
  });

  it('should not deduplicate values for changing parent keys', () => {
    const rows = [
      { controller: 'FooController', action: 'index', method: 'GET' },
      { controller: 'FooController', action: 'index', method: 'POST' },
      { controller: 'BarController', action: 'index', method: 'GET' },
      { controller: 'BarController', action: 'index', method: 'POST' },
    ];
    const result = deduplicateValues(rows, ['controller', 'action', 'method']);

    expect(result).toEqual([
      { controller: 'FooController', action: 'index', method: 'GET' },
      { method: 'POST' },
      { controller: 'BarController', action: 'index', method: 'GET' },
      { method: 'POST' },
    ]);
  });

  it('should not deduplicate values for different parent keys', () => {
    const rows = [
      { controller: 'FooController', action: 'index', method: 'GET' },
      { controller: 'BarController', action: 'index', method: 'GET' },
    ];
    const result = deduplicateValues(rows, ['controller', 'action', 'method']);

    expect(result).toEqual([
      { controller: 'FooController', action: 'index', method: 'GET' },
      { controller: 'BarController', action: 'index', method: 'GET' },
    ]);
  });
});
