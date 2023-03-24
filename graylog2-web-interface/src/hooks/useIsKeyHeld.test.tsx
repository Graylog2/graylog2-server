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
import { fireEvent, render } from 'wrappedTestingLibrary';
import { renderHook, act } from 'wrappedTestingLibrary/hooks';

import useIsKeyHeld from 'hooks/useIsKeyHeld';

describe('useIsKeyHeld custom hook', () => {
  it('Test is Enter key held after keyDown and upheld after keyUp', async () => {
    const { container } = render(<input />);
    const { result } = renderHook(() => useIsKeyHeld('Enter'));

    // eslint-disable-next-line testing-library/no-unnecessary-act
    act(() => {
      fireEvent.keyDown(container, { key: 'Enter', code: 13, charCode: 13 });
    });

    expect(result.current).toEqual(true);

    // eslint-disable-next-line testing-library/no-unnecessary-act
    act(() => {
      fireEvent.keyUp(container, { key: 'Enter', code: 13, charCode: 13 });
    });

    expect(result.current).toEqual(false);
  });

  it('Test is Enter key held when user click other key', () => {
    const { result } = renderHook(() => useIsKeyHeld('Enter'));
    const { container } = render(<input />);

    // eslint-disable-next-line testing-library/no-unnecessary-act
    act(() => {
      fireEvent.keyDown(container, { key: 'Enter', code: 13, charCode: 13 });
    });

    expect(result.current).toEqual(true);

    // eslint-disable-next-line testing-library/no-unnecessary-act
    act(() => {
      fireEvent.keyDown(container, { key: 'Shift', code: 16, charCode: 16 });
    });

    expect(result.current).toEqual(true);
  });
});
