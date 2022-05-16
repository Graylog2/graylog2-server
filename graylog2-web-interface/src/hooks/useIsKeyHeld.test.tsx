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
import React from 'react';
import { renderHook, act } from '@testing-library/react-hooks';
import { fireEvent, render } from 'wrappedTestingLibrary';
/* eslint-disable testing-library/no-unnecessary-act */

import useIsKeyHeld from 'hooks/useIsKeyHeld';

describe('useIsKeyHeld custom hook', () => {
  it('Test is Enter key held after keyDown and upheld after keyUp', async () => {
    const { container } = render(<input />);
    const { result } = renderHook(() => useIsKeyHeld('Enter'));

    act(() => {
      fireEvent.keyDown(container, { key: 'Enter', code: 13, charCode: 13 });
    });

    expect(result.current).toEqual(true);

    act(() => {
      fireEvent.keyUp(container, { key: 'Enter', code: 13, charCode: 13 });
    });

    expect(result.current).toEqual(false);
  });

  it('Test is Enter key held when user click other key', () => {
    const { result } = renderHook(() => useIsKeyHeld('Enter'));
    const { container } = render(<input />);

    act(() => {
      fireEvent.keyDown(container, { key: 'Enter', code: 13, charCode: 13 });
    });

    expect(result.current).toEqual(true);

    act(() => {
      fireEvent.keyDown(container, { key: 'Shift', code: 16, charCode: 16 });
    });

    expect(result.current).toEqual(true);
  });
});
