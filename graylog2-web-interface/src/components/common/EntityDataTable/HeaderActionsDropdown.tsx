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
import styled, { css } from 'styled-components';

import Menu from 'components/bootstrap/Menu';
import { MenuAnchor } from 'components/bootstrap/useClickToOpenMenu';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { MenuItem } from 'components/bootstrap';
import Icon from 'components/common/Icon';

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

export const DropdownCaret = styled(Icon)`
  opacity: 0;
  transition: opacity 0.15s ease-in-out;
`;

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
  textAlign: 'right' | 'left';
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
  textAlign,
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
      <DropdownTrigger title={`Toggle ${label} actions`}>
        {textAlign === 'right' && <DropdownCaret name="arrow_drop_down" size="xs" className="header-action" />}
        {children}
        {textAlign !== 'right' && <DropdownCaret name="arrow_drop_down" size="xs" className="header-action" />}
      </DropdownTrigger>

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
