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
import userEvent from '@testing-library/user-event';
import { render } from 'wrappedTestingLibrary';
import { renderHook } from 'wrappedTestingLibrary/hooks';

import useIsKeyHeld from 'hooks/useIsKeyHeld';

describe('useIsKeyHeld custom hook', () => {
  it('Test is Enter key held after keyDown and upheld after keyUp', async () => {
    render(<input />);
    const { result } = renderHook(() => useIsKeyHeld('Enter'));

    const keyboardState = await userEvent.keyboard('{Enter>}');

    expect(result.current).toEqual(true);

    await userEvent.keyboard('{/Enter}', { keyboardState });

    expect(result.current).toEqual(false);
  });

  it('Test is Enter key held when user click other key', async () => {
    const { result } = renderHook(() => useIsKeyHeld('Enter'));
    render(<input />);

    const keyboardState = await userEvent.keyboard('{Enter>}');

    expect(result.current).toEqual(true);

    await userEvent.keyboard('{Shift>}', { keyboardState });

    expect(result.current).toEqual(true);
  });
});
