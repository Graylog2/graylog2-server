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
import { act, renderHook } from 'wrappedTestingLibrary/hooks';

import useSidebarWidth from './useSidebarWidth';

const STORAGE_KEY = 'sidebar-width.outline';
const BOUNDS = { initial: 236, min: 180, max: 480 };

describe('useSidebarWidth', () => {
  beforeEach(() => localStorage.clear());

  it('starts at the initial width and keeps changes in local storage', () => {
    const { result } = renderHook(() => useSidebarWidth(STORAGE_KEY, BOUNDS));

    expect(result.current[0]).toBe(236);

    act(() => result.current[1](300.4));

    expect(result.current[0]).toBe(300);
    expect(JSON.parse(localStorage.getItem(STORAGE_KEY))).toBe(300);
    expect(renderHook(() => useSidebarWidth(STORAGE_KEY, BOUNDS)).result.current[0]).toBe(300);
  });

  it('keeps widths within the bounds', () => {
    const { result } = renderHook(() => useSidebarWidth(STORAGE_KEY, BOUNDS));

    act(() => result.current[1](50));

    expect(result.current[0]).toBe(180);

    act(() => result.current[1](2000));

    expect(result.current[0]).toBe(480);
  });

  it('ignores stored widths that are not numbers and clamps those out of bounds', () => {
    localStorage.setItem(STORAGE_KEY, '"wide"');

    expect(renderHook(() => useSidebarWidth(STORAGE_KEY, BOUNDS)).result.current[0]).toBe(236);

    localStorage.setItem(STORAGE_KEY, '9000');

    expect(renderHook(() => useSidebarWidth(STORAGE_KEY, BOUNDS)).result.current[0]).toBe(480);
  });
});
