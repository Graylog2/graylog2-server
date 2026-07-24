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
import { forwardRef } from 'react';
import { createPortal } from 'react-dom';
import styled, { css } from 'styled-components';

import Menu from 'components/bootstrap/Menu';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { MenuItem } from 'components/bootstrap';

// A plain (non-focusable) span: the header cell itself (ThInner, see AttributeHeader) is the one
// focusable/clickable element for opening this menu, so this only renders the label -- it must
// not be its own tab stop, or focusing the header would take two Tab presses instead of one.
const DropdownTrigger = styled.span(
  ({ theme }) => css`
    display: inline-flex;
    align-items: center;
    gap: ${theme.spacings.xxs};
    line-height: inherit;
  `,
);

const StyledMenuAnchor = styled.div`
  position: fixed;
  width: 0;
  height: 0;
  pointer-events: none;
`;

// Zero-size and invisible: it only exists to be the Menu's positioning anchor, moved to the
// clicked point (see AttributeHeader) so the dropdown opens exactly where the header was clicked,
// rather than anchored to the DropdownTrigger's own position.
//
// Rendered through a portal straight into <body>: the header (Th) always has a CSS `transform`
// set (even when idle, it falls back to `translate3d(0, 0, 0)` -- see TableHead.tsx), and any
// transformed ancestor becomes the containing block for a `position: fixed` descendant. Left in
// place, this anchor's "fixed" coordinates would resolve relative to that header's own box
// instead of the viewport, landing nowhere near the actual click.
const MenuAnchor = forwardRef<HTMLDivElement, { style?: React.CSSProperties }>(({ style = undefined, ...rest }, ref) =>
  createPortal(<StyledMenuAnchor ref={ref} style={style} {...rest} />, document.body),
);

const MenuItemLabel = styled.span<{ $active: boolean }>(
  ({ $active }) => css`
    font-weight: ${$active ? 'bold' : 'inherit'};
  `,
);

type Props = {
  children: React.ReactNode;
  label: string;
  activeSort?: 'asc' | 'desc' | false;
  isSliceActive?: boolean;
  onChangeSlicing: (sliceCol: string | undefined, slice?: string) => void;
  sliceColumnId?: string;
  appSection?: string;
  onSort?: (desc: boolean) => void;
  onHideColumn?: () => void;
  opened?: boolean;
  onOpenChange?: (opened: boolean) => void;
  anchorPosition?: { x: number; y: number } | null;
};

const HeaderActionsDropdown = ({
  children,
  label,
  activeSort = false,
  isSliceActive = false,
  onChangeSlicing,
  sliceColumnId = undefined,
  appSection = undefined,
  onSort = undefined,
  onHideColumn = undefined,
  opened = undefined,
  onOpenChange = undefined,
  anchorPosition = undefined,
}: Props) => {
  const sendTelemetry = useSendTelemetry();
  const hasActions = Boolean(onChangeSlicing || onSort || onHideColumn);

  const onToggleSlicing = () => {
    if (isSliceActive) {
      sendTelemetry(TELEMETRY_EVENT_TYPE.ENTITY_DATA_TABLE.SLICE_REMOVED, {
        app_section: appSection,
        app_action_value: 'slice-remove',
        event_details: { attribute_id: sliceColumnId },
      });

      return onChangeSlicing(undefined, undefined);
    }
    sendTelemetry(TELEMETRY_EVENT_TYPE.ENTITY_DATA_TABLE.SLICE_COLUMN_SELECTED_HEADER, {
      app_section: appSection,
      app_action_value: 'slice-column-header',
      event_details: { attribute_id: sliceColumnId },
    });

    return onChangeSlicing(sliceColumnId);
  };

  if (!hasActions) {
    return <>{children}</>;
  }

  return (
    <Menu shadow="md" withinPortal position="bottom-start" opened={opened} onChange={onOpenChange}>
      <Menu.Target>
        <MenuAnchor style={{ left: anchorPosition?.x ?? 0, top: anchorPosition?.y ?? 0 }} />
      </Menu.Target>
      {/* Not the Menu.Target: opening/positioning is driven by the whole header's click handler
          (see AttributeHeader) so this only needs to render the label -- clicking it still opens
          the menu, since the click bubbles up to that handler. */}
      <DropdownTrigger title={`Toggle ${label} actions`}>{children}</DropdownTrigger>
      <Menu.Dropdown>
        {onSort && (
          <MenuItem onClick={() => onSort(false)} icon="arrow_upward">
            <MenuItemLabel $active={activeSort === 'asc'}>Sort ascending</MenuItemLabel>
          </MenuItem>
        )}
        {onSort && (
          <MenuItem onClick={() => onSort(true)} icon="arrow_downward">
            <MenuItemLabel $active={activeSort === 'desc'}>Sort descending</MenuItemLabel>
          </MenuItem>
        )}
        {onSort && onChangeSlicing && <MenuItem divider />}
        {onChangeSlicing && (
          <MenuItem onClick={onToggleSlicing} icon="surgical">
            {isSliceActive ? 'No slicing' : 'Slice by values'}
          </MenuItem>
        )}
        {(onSort || onChangeSlicing) && onHideColumn && <MenuItem divider />}
        {onHideColumn && (
          <MenuItem onClick={onHideColumn} icon="visibility_off">
            Hide column
          </MenuItem>
        )}
      </Menu.Dropdown>
    </Menu>
  );
};

export default HeaderActionsDropdown;
