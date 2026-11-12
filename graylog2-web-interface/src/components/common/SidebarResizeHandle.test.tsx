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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import SidebarResizeHandle from './SidebarResizeHandle';

const BOUNDS = { initial: 384, min: 300, max: 720 };

const renderHandle = (side: 'left' | 'right', onResize = jest.fn()) => {
  render(<SidebarResizeHandle side={side} width={400} bounds={BOUNDS} label="Resize Assistant" onResize={onResize} />);

  return onResize;
};

describe('<SidebarResizeHandle />', () => {
  beforeAll(() => {
    // jsdom has no PointerEvent; without it, pointer events carry no coordinates.
    if (!window.PointerEvent) {
      (window as { PointerEvent: unknown }).PointerEvent = window.MouseEvent;
    }

    HTMLElement.prototype.setPointerCapture = jest.fn();
    HTMLElement.prototype.releasePointerCapture = jest.fn();
  });

  it('widens a right sidebar when dragged to the left', async () => {
    const onResize = renderHandle('right');
    const handle = await screen.findByRole('separator', { name: 'Resize Assistant' });

    await userEvent.pointer([
      { keys: '[MouseLeft>]', target: handle, coords: { clientX: 1000 } },
      { target: handle, coords: { clientX: 950 } },
      { keys: '[/MouseLeft]', target: handle },
      { target: handle, coords: { clientX: 900 } },
    ]);

    expect(onResize).toHaveBeenCalledTimes(1);
    expect(onResize).toHaveBeenCalledWith(450);
  });

  it('widens a left sidebar when dragged to the right', async () => {
    const onResize = renderHandle('left');
    const handle = await screen.findByRole('separator', { name: 'Resize Assistant' });

    await userEvent.pointer([
      { keys: '[MouseLeft>]', target: handle, coords: { clientX: 200 } },
      { target: handle, coords: { clientX: 260 } },
    ]);

    expect(onResize).toHaveBeenCalledWith(460);
  });

  it('resizes with the arrow keys and resets on double click', async () => {
    const onResize = renderHandle('right');
    const handle = await screen.findByRole('separator', { name: 'Resize Assistant' });

    handle.focus();
    await userEvent.keyboard('{ArrowLeft}');

    expect(onResize).toHaveBeenLastCalledWith(416);

    await userEvent.dblClick(handle);

    expect(onResize).toHaveBeenLastCalledWith(384);
    expect(handle).toHaveAttribute('aria-valuenow', '400');
  });
});
