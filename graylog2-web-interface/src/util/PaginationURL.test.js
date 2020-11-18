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
import PaginationURL from './PaginationURL';

describe('PaginationUR', () => {
  it('should create a pagination url without query', () => {
    const url = PaginationURL('https://foo', 1, 10);

    expect(url).toEqual('https://foo/?page=1&per_page=10');
  });

  it('should create a pagination url with query', () => {
    const url = PaginationURL('https://foo', 1, 10, 'bar');

    expect(url).toEqual('https://foo/?page=1&per_page=10&query=bar');
  });

  it('should create a pagination url with query addition field', () => {
    const url = PaginationURL('https://foo', 1, 10, 'bar', { resolve: false });

    expect(url).toEqual('https://foo/?page=1&per_page=10&resolve=false&query=bar');
  });

  it('should create a pagination url with query addition fields', () => {
    const url = PaginationURL('https://foo', 1, 10, 'bar',
      { bool: false, number: 12, string: 'string', double: 1.2, object: { toString: () => 'object' } });

    expect(url).toEqual('https://foo/?page=1&per_page=10&bool=false&number=12&string=string&double=1.2&object=object&query=bar');
  });
});
