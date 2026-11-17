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
import { extractFieldLimitErrors } from './fieldLimit';

describe('extractFieldLimitErrors', () => {
  it('reads the path and message list of the index set endpoints', () => {
    const error = { additional: { body: [{ path: 'field_limit', message: 'must be at least 1000' }] } };

    expect(extractFieldLimitErrors(error)).toEqual({ field_limit: 'must be at least 1000' });
  });

  it('reads the single message of the template endpoints', () => {
    const error = {
      additional: { body: { message: 'Invalid value for field [fieldLimit]: must be greater than or equal to 1000' } },
    };

    expect(extractFieldLimitErrors(error)).toEqual({ field_limit: 'must be greater than or equal to 1000' });
  });

  it('ignores errors that belong to other fields', () => {
    expect(extractFieldLimitErrors({ additional: { body: { message: 'Invalid value for field [shards]: bad' } } })).toEqual(
      {},
    );
  });
});
