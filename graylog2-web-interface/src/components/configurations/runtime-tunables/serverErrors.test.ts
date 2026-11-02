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

import FetchError from 'logic/errors/FetchError';

import { toFormErrors } from './serverErrors';

describe('toFormErrors', () => {
  it('maps a 400 validation body to nested field errors and global errors', () => {
    const error = new FetchError('Bad Request', 400, {
      failed: true,
      errors: {
        'entries.1.value': ['must not be blank'],
        min_length: ['too small', 'really'],
        _global: ['invalid combination'],
      },
      error_context: {},
    });

    expect(toFormErrors(error)).toEqual({
      fieldErrors: { entries: [undefined, { value: 'must not be blank' }], min_length: 'too small, really' },
      globalErrors: ['invalid combination'],
    });
  });

  it('ignores other errors', () => {
    expect(toFormErrors(new FetchError('Server Error', 500, { message: 'boom' }))).toBeUndefined();
    expect(toFormErrors(new Error('boom'))).toBeUndefined();
  });
});
