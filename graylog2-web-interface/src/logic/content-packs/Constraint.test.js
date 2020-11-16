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
import { Set } from 'immutable';

import Constraint from './Constraint';

describe('Constraint', () => {
  const constraint1 = Constraint.builder()
    .type('server')
    .version('3.0.0')
    .build();

  const constraint2 = Constraint.builder()
    .type('server')
    .version('3.0.0')
    .build();

  const constraint3 = Constraint.builder()
    .type('plugin')
    .version('3.0.0')
    .plugin('graylog.plugin.foo')
    .build();

  it('should be add to a Set without duplication', () => {
    const set = Set().add(constraint1)
      .add(constraint2)
      .add(constraint3);

    expect(set.size).toBe(2);
  });

  it('should be equaling it self', () => {
    expect(constraint1.equals(constraint2)).toBe(true);
    expect(constraint1.equals(constraint3)).toBe(false);
    expect(constraint1.equals({ version: '3.0.0', type: 'server', plugin: 'server' })).toBe(true);
  });
});
