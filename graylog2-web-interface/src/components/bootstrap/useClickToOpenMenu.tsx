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
import { forwardRef, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import styled from 'styled-components';

type Position = { x: number; y: number };

const StyledMenuAnchor = styled.div`
  position: fixed;
  width: 0;
  height: 0;
  pointer-events: none;
`;

// Invisible positioning anchor for a Menu's `Menu.Target`, moved to the click point; portaled into
// <body> so a `transform` on some ancestor can't turn its `position: fixed` coordinates relative.
export const MenuAnchor = forwardRef<HTMLDivElement, { style?: React.CSSProperties }>(
  ({ style = undefined, ...rest }, ref) =>
    createPortal(<StyledMenuAnchor ref={ref} style={style} {...rest} />, document.body),
);

// Makes a whole element (not just a dedicated toggle button) act as a dropdown trigger: click or
// Enter opens a Mantine `Menu` at that point, a second click/Enter closes it, and closing it for
// any reason returns focus to the trigger; rendering the `<Menu>` itself is left to the caller.
const useClickToOpenMenu = <Trigger extends HTMLElement = HTMLElement>() => {
  const triggerRef = useRef<Trigger>(null);
  const [opened, setOpened] = useState(false);
  const [anchorPosition, setAnchorPosition] = useState<Position | null>(null);

  // Mantine's own focus-return fires too late here (its FocusTrap moves focus into the dropdown
  // before that capture runs), so we return focus to the trigger on close ourselves.
  const onOpenChange = (nextOpened: boolean) => {
    setOpened(nextOpened);

    if (!nextOpened) {
      triggerRef.current?.focus({ preventScroll: true });
    }
  };

  const toggleAt = (position: Position) => {
    if (opened) {
      onOpenChange(false);

      return;
    }

    setAnchorPosition(position);
    setOpened(true);
  };

  const onClick = (event: React.MouseEvent<Trigger>) => {
    // An assistive-tech-triggered click reports (0, 0) here, so fall back to the trigger's rect.
    const { clientX, clientY } = event;
    const hasPointerPosition = clientX !== 0 || clientY !== 0;
    const rect = event.currentTarget.getBoundingClientRect();

    toggleAt(hasPointerPosition ? { x: clientX, y: clientY } : { x: rect.left, y: rect.bottom });
  };

  // Ignore keydowns bubbled up from a focused descendant (e.g. a menu item), only the trigger itself.
  const onKeyDown = (event: React.KeyboardEvent<Trigger>) => {
    if (event.target !== event.currentTarget || event.key !== 'Enter') {
      return;
    }

    event.preventDefault();
    const rect = event.currentTarget.getBoundingClientRect();
    toggleAt({ x: rect.left, y: rect.bottom });
  };

  return { triggerRef, opened, onOpenChange, anchorPosition, onClick, onKeyDown };
};

export default useClickToOpenMenu;
