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
import * as React from 'react';
import { renderHook } from 'wrappedTestingLibrary/hooks';

import type { KeyMapper } from 'views/components/visualizations/TransformKeys';

import useMapKeys from './useMapKeys';
import KeyMapperContext from './KeyMapperContext';

describe('useMapKeys', () => {
  it('returns the identity mapper when no provider is present', () => {
    const { result } = renderHook(() => useMapKeys());

    expect(result.current('raw-id', 'streams')).toBe('raw-id');
  });

  it('returns the mapper supplied by KeyMapperContext', () => {
    const mapper: KeyMapper = (key, field) => `${field}:${key}`;
    const wrapper = ({ children }: React.PropsWithChildren) => (
      <KeyMapperContext.Provider value={mapper}>{children}</KeyMapperContext.Provider>
    );

    const { result } = renderHook(() => useMapKeys(), { wrapper });

    expect(result.current('stream-1', 'streams')).toBe('streams:stream-1');
  });
});
