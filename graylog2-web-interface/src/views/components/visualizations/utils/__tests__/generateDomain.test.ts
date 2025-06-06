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

import generateDomain from 'views/components/visualizations/utils/generateDomain';

describe('generateDomain', () => {
  it('generate domain for 1 axis', () => {
    const result = generateDomain(1);

    expect(result).toEqual([0, 1]);
  });

  it('generate domain for 2 axis', () => {
    const result = generateDomain(2);

    expect(result).toEqual([0, 1]);
  });

  it('generate domain for 3 axis', () => {
    const result = generateDomain(3);

    expect(result).toEqual([0.1, 1]);
  });

  it('generate domain for 4 axis', () => {
    const result = generateDomain(4);

    expect(result).toEqual([0.1, 0.9]);
  });
});
