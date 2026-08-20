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

import extractErrorMessage from './extractErrorMessage';

describe('extractErrorMessage', () => {
  it('returns responseMessage from a FetchError when present', () => {
    const err = new FetchError('boom', 400, { message: 'invalid input on field X' });

    expect(extractErrorMessage(err)).toBe('invalid input on field X');
  });

  it('falls back to FetchError.message when responseMessage is undefined', () => {
    const err = new FetchError('boom', 500, undefined);

    expect(extractErrorMessage(err)).toContain('boom');
  });

  it('returns message from a generic Error', () => {
    expect(extractErrorMessage(new Error('something broke'))).toBe('something broke');
  });

  it('returns the value when given a string', () => {
    expect(extractErrorMessage('plain message')).toBe('plain message');
  });

  it('stringifies plain objects without crashing', () => {
    expect(extractErrorMessage({ some: 'object' })).toBe('[object Object]');
  });

  it('handles undefined / null', () => {
    expect(extractErrorMessage(undefined)).toBe('undefined');
    expect(extractErrorMessage(null)).toBe('null');
  });
});
