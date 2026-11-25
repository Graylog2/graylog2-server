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
import { useRef, useState } from 'react';
import styled, { css } from 'styled-components';

import type { SidebarBounds } from 'hooks/useSidebarSize';

const KEYBOARD_STEP = 16;
// How far beyond its smallest size the user drags before e.g. a sidebar collapses, so resizing it to its limit does
// not collapse it by accident.
const DRAG_PAST_LIMIT = 80;

type Side = 'left' | 'right' | 'top' | 'bottom';

const isVertical = (side: Side) => side === 'top' || side === 'bottom';

const BAR_HEIGHT = 8;

// Left and right sidebars resize from an invisible edge over their border, top and bottom ones from a visible bar
// with a grip, placed between them and what they are resized against.
const Handle = styled.div<{ $side: Side }>(
  ({ theme, $side }) => css`
    touch-action: none;

    ${isVertical($side)
      ? css`
          display: flex;
          flex-shrink: 0;
          align-items: center;
          justify-content: center;
          height: ${BAR_HEIGHT}px;
          cursor: row-resize;
          background-color: ${theme.colors.global.background};
          border: 1px solid ${theme.colors.cards.border};

          &::before {
            content: '';
            width: 32px;
            height: 2px;
            border-radius: 1px;
            background: ${theme.colors.text.secondary};
          }
        `
      : css`
          position: absolute;
          top: 0;
          bottom: 0;
          ${$side === 'left' ? 'right' : 'left'}: -3px;
          z-index: 2;
          width: 6px;
          cursor: col-resize;
        `}

    &:hover,
    &:focus-visible,
    &[data-dragging='true'] {
      background: ${theme.colors.input.borderFocus};
      outline: none;
    }
  `,
);

type Props = {
  // The side of the page the sidebar is on; dragging away from it enlarges the sidebar.
  side: Side;
  // The sidebar's size along the drag direction, its height for `top` and `bottom`.
  size: number;
  bounds: SidebarBounds;
  label: string;
  onResize: (size: number) => void;
  // Called when dragged far past the smallest size, e.g. to collapse a sidebar. The size goes back to the one before
  // the drag.
  onDragPastMin?: () => void;
};

/**
 * Resizes a sidebar by dragging or with the arrow keys; a double click restores its initial size.
 * Place a left or right handle inside the sidebar's positioned container, a top or bottom one next to the sidebar.
 */
const SidebarResizeHandle = ({ side, size, bounds, label, onResize, onDragPastMin = undefined }: Props) => {
  const drag = useRef<{ pointerId: number; start: number; startSize: number } | undefined>(undefined);
  const [dragging, setDragging] = useState(false);
  const vertical = isVertical(side);
  const direction = side === 'left' || side === 'top' ? 1 : -1;
  const position = (event: React.PointerEvent) => (vertical ? event.clientY : event.clientX);

  const onPointerDown = (event: React.PointerEvent<HTMLDivElement>) => {
    if (drag.current || event.button !== 0) return;

    event.preventDefault();
    event.currentTarget.setPointerCapture(event.pointerId);
    drag.current = { pointerId: event.pointerId, start: position(event), startSize: size };
    setDragging(true);
  };

  const endDrag = (event: React.PointerEvent<HTMLDivElement>) => {
    if (drag.current && drag.current.pointerId === event.pointerId) {
      event.currentTarget.releasePointerCapture(event.pointerId);
      drag.current = undefined;
      setDragging(false);
    }
  };

  const onPointerMove = (event: React.PointerEvent<HTMLDivElement>) => {
    if (!drag.current || drag.current.pointerId !== event.pointerId) return;

    const { startSize } = drag.current;
    const draggedSize = startSize + direction * (position(event) - drag.current.start);

    if (onDragPastMin && draggedSize < bounds.min - DRAG_PAST_LIMIT) {
      endDrag(event);
      onResize(startSize);
      onDragPastMin();

      return;
    }

    onResize(draggedSize);
  };

  const onKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    const steps = vertical
      ? { ArrowUp: -KEYBOARD_STEP, ArrowDown: KEYBOARD_STEP }
      : { ArrowLeft: -KEYBOARD_STEP, ArrowRight: KEYBOARD_STEP };
    const step = steps[event.key];

    if (step !== undefined) {
      event.preventDefault();
      onResize(size + direction * step);
    }
  };

  return (
    <Handle
      $side={side}
      role="separator"
      aria-orientation={vertical ? 'horizontal' : 'vertical'}
      aria-label={label}
      aria-valuenow={size}
      aria-valuemin={bounds.min}
      aria-valuemax={bounds.max}
      tabIndex={0}
      title={`${label}. Double-click to reset.`}
      data-dragging={dragging}
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={endDrag}
      onPointerCancel={endDrag}
      onDoubleClick={() => onResize(bounds.initial)}
      onKeyDown={onKeyDown}
    />
  );
};

export default SidebarResizeHandle;
