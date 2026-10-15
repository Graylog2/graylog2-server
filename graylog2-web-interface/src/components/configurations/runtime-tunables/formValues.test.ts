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
  it('picks the values of the schema properties', () => {
    const schema: TunableSchema = {
      type: 'object',
      properties: {
        name: { type: 'string' },
        limit: { type: 'integer' },
      },
    };

    expect(toFormValues(schema, { name: 'x', limit: 5, unknown: true })).toEqual({ name: 'x', limit: 5 });
  });
});

describe('changedProperties', () => {
  it('lists properties whose value changed', () => {
    expect(changedProperties({ limit: 5, enabled: true }, { limit: 6, enabled: true })).toEqual(['limit']);
  });
});
