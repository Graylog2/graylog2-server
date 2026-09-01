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
import { OTHER_BUCKET_NAME } from 'views/Constants';

import containsOtherBucketValue from './containsOtherBucketValue';

describe('containsOtherBucketValue', () => {
  it('returns false when there is no value and no valuePath', () => {
    expect(containsOtherBucketValue(undefined, undefined)).toBe(false);
    expect(containsOtherBucketValue('some-value', {})).toBe(false);
  });

  it('returns true when the plain value is the Other bucket', () => {
    expect(containsOtherBucketValue(OTHER_BUCKET_NAME, undefined)).toBe(true);
  });

  it('returns false when the plain value is a regular value', () => {
    expect(containsOtherBucketValue('http_method', undefined)).toBe(false);
  });

  it('returns true when any valuePath entry contains the Other bucket', () => {
    const contexts = { valuePath: [{ source: 'a' }, { http_method: OTHER_BUCKET_NAME }] };

    expect(containsOtherBucketValue(undefined, contexts)).toBe(true);
  });

  it('returns false when no valuePath entry contains the Other bucket', () => {
    const contexts = { valuePath: [{ source: 'a' }, { http_method: 'GET' }] };

    expect(containsOtherBucketValue(undefined, contexts)).toBe(false);
  });
});
