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

import { renderHook } from '@testing-library/react-hooks';

import useIsMountedRef from './useIsMountedRef';

describe('useIsMountedRef', () => {
  it('should be true after mount', () => {
    const { result: { current: isMountedRef } } = renderHook(useIsMountedRef);

    expect(isMountedRef.current).toEqual(true);
  });

  it('should be false after unmount', () => {
    const { unmount, result: { current: isMountedRef } } = renderHook(useIsMountedRef);

    expect(isMountedRef.current).toEqual(true);

    unmount();

    expect(isMountedRef.current).toEqual(false);
  });
});
