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

// Zero-size and invisible: it only exists to be a Menu's positioning anchor, moved to the clicked
// point (see useClickToOpenMenu below) so the dropdown opens exactly where its trigger element was
// clicked, rather than anchored to the trigger's own position. Render it inside that Menu's
// `Menu.Target`.
//
// Rendered through a portal straight into <body>: a CSS `transform` on any ancestor of this anchor
// becomes the containing block for its `position: fixed`, which would resolve its coordinates
// relative to that ancestor's own box instead of the viewport. Portaling past all of them avoids
// having to know whether some particular ancestor happens to have one.
export const MenuAnchor = forwardRef<HTMLDivElement, { style?: React.CSSProperties }>(
  ({ style = undefined, ...rest }, ref) =>
    createPortal(<StyledMenuAnchor ref={ref} style={style} {...rest} />, document.body),
);

/**
 * Wires up a "click anywhere on this element to open a Mantine `Menu` right where you clicked"
 * interaction, for making a whole element (not just a dedicated toggle button) act as a dropdown
 * trigger. A click or an Enter keypress on the returned `triggerRef` element opens the menu
 * anchored at that point (render `<MenuAnchor style={{ left: anchorPosition?.x ?? 0, top:
 * anchorPosition?.y ?? 0 }} />` inside the Menu's `Menu.Target`); a second click/Enter closes it
 * again; and closing it for any reason returns focus to the trigger element.
 *
 * Only handles the open/close/position state and the click/keydown wiring -- rendering the actual
 * `<Menu>`/`<Menu.Dropdown>` content, and deciding whether the trigger element has anything to
 * toggle in the first place, is left to the caller.
 */
const useClickToOpenMenu = <Trigger extends HTMLElement = HTMLElement>() => {
  const triggerRef = useRef<Trigger>(null);
  const [opened, setOpened] = useState(false);
  const [anchorPosition, setAnchorPosition] = useState<Position | null>(null);

  // Mantine's own dropdowns return focus to the element that opened them once they close; its
  // built-in mechanism for that (Popover's `returnFocus`) just remembers `document.activeElement`
  // at open time and refocuses it at close time -- but Menu's own FocusTrap moves focus into the
  // dropdown (onto its first item) before that capture runs, so by then it's too late: it ends up
  // "returning" focus to something inside the dropdown instead of the trigger. Doing it ourselves is
  // simple and matches that same "usual dropdown" behavior: closing the menu, for any reason (an
  // action, Escape, an outside click, or toggling it via the trigger again), always returns focus to
  // the trigger element itself.
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
    // A keyboard-triggered click (e.g. assistive tech activating a `role="button"` element)
    // reports (0, 0) here, so fall back to anchoring at the trigger's own position instead of the
    // top-left of the viewport.
    const { clientX, clientY } = event;
    const hasPointerPosition = clientX !== 0 || clientY !== 0;
    const rect = event.currentTarget.getBoundingClientRect();

    toggleAt(hasPointerPosition ? { x: clientX, y: clientY } : { x: rect.left, y: rect.bottom });
  };

  // Only for a keydown on the trigger itself, not one that bubbled up from a focused descendant
  // (e.g. a focused menu item -- since Menu.Dropdown is rendered through a portal into <body>,
  // React's synthetic events still bubble along the *React* tree, not the DOM tree). Otherwise
  // Enter on a descendant would toggle this menu instead of letting its own onClick run.
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
