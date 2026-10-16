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

import { changedProperties, toFormValues } from './formValues';
import type { TunableSchema } from './types';

describe('toFormValues', () => {
  it('turns secrets into keep or set values and leaves the rest', () => {
    const schema: TunableSchema = {
      type: 'object',
      properties: {
        name: { type: 'string' },
        token: { type: 'string', 'x-graylog-secret': true },
        other: { type: 'string', 'x-graylog-secret': true },
      },
    };

    expect(toFormValues(schema, { name: 'x', token: { is_set: true }, other: { is_set: false } })).toEqual({
      name: 'x',
      token: { keep_value: true },
      other: { set_value: '' },
    });
  });
});

describe('changedProperties', () => {
  it('lists properties whose value changed, counting kept secrets as unchanged', () => {
    expect(
      changedProperties(
        { limit: 5, enabled: true, token: { keep_value: true }, other: { keep_value: true } },
        { limit: 6, enabled: true, token: { keep_value: true }, other: { set_value: 'new' } },
      ),
    ).toEqual(['limit', 'other']);
  });
});
