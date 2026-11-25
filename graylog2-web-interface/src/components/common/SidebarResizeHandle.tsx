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

import type { SidebarBounds } from 'hooks/useSidebarWidth';

const KEYBOARD_STEP = 16;

type Side = 'left' | 'right' | 'top' | 'bottom';

const isVertical = (side: Side) => side === 'top' || side === 'bottom';

const OPPOSITE_EDGE = { left: 'right', right: 'left', top: 'bottom', bottom: 'top' } as const;

// Sits on the sidebar's inner edge, over its border.
const Handle = styled.div<{ $side: Side }>(
  ({ theme, $side }) => css`
    position: absolute;
    ${OPPOSITE_EDGE[$side]}: -3px;
    z-index: 2;
    touch-action: none;

    ${isVertical($side)
      ? css`
          left: 0;
          right: 0;
          height: 6px;
          cursor: row-resize;
        `
      : css`
          top: 0;
          bottom: 0;
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
  width: number;
  bounds: SidebarBounds;
  label: string;
  onResize: (width: number) => void;
};

/**
 * Resizes a sidebar by dragging or with the arrow keys; a double click restores its initial size.
 * Place it inside the sidebar's positioned container.
 */
const SidebarResizeHandle = ({ side, width, bounds, label, onResize }: Props) => {
  const drag = useRef<{ pointerId: number; start: number; startWidth: number } | undefined>(undefined);
  const [dragging, setDragging] = useState(false);
  const vertical = isVertical(side);
  const direction = side === 'left' || side === 'top' ? 1 : -1;
  const position = (event: React.PointerEvent) => (vertical ? event.clientY : event.clientX);

  const onPointerDown = (event: React.PointerEvent<HTMLDivElement>) => {
    if (drag.current || event.button !== 0) return;

    event.preventDefault();
    event.currentTarget.setPointerCapture(event.pointerId);
    drag.current = { pointerId: event.pointerId, start: position(event), startWidth: width };
    setDragging(true);
  };

  const onPointerMove = (event: React.PointerEvent<HTMLDivElement>) => {
    if (drag.current && drag.current.pointerId === event.pointerId) {
      onResize(drag.current.startWidth + direction * (position(event) - drag.current.start));
    }
  };

  const endDrag = (event: React.PointerEvent<HTMLDivElement>) => {
    if (drag.current && drag.current.pointerId === event.pointerId) {
      event.currentTarget.releasePointerCapture(event.pointerId);
      drag.current = undefined;
      setDragging(false);
    }
  };

  const onKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    const steps = vertical
      ? { ArrowUp: -KEYBOARD_STEP, ArrowDown: KEYBOARD_STEP }
      : { ArrowLeft: -KEYBOARD_STEP, ArrowRight: KEYBOARD_STEP };
    const step = steps[event.key];

    if (step !== undefined) {
      event.preventDefault();
      onResize(width + direction * step);
    }
  };

  return (
    <Handle
      $side={side}
      role="separator"
      aria-orientation={vertical ? 'horizontal' : 'vertical'}
      aria-label={label}
      aria-valuenow={width}
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
