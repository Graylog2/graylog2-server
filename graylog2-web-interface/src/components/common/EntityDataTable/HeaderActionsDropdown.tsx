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
import Icon from 'components/common/Icon';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { MenuItem } from 'components/bootstrap';

const DropdownTrigger = styled.button(
  ({ theme }) => css`
    background: transparent;
    border: 0;
    padding: 0;
    display: inline-flex;
    align-items: center;
    gap: ${theme.spacings.xxs};
    line-height: normal;

    &:focus-visible {
      outline-offset: 2px;
    }
  `,
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
}: Props) => {
  const sendTelemetry = useSendTelemetry();
  const hasActions = Boolean(onChangeSlicing || onSort);

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
    <Menu shadow="md" withinPortal position="bottom-start">
      <Menu.Target>
        <DropdownTrigger type="button" title={`Toggle ${label} actions`} aria-label={`Toggle ${label} actions`}>
          <span>{children}</span>
          <Icon name="arrow_drop_down" size="xs" />
        </DropdownTrigger>
      </Menu.Target>
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
            {isSliceActive ? 'Remove slicing' : 'Slice by values'}
          </MenuItem>
        )}
      </Menu.Dropdown>
    </Menu>
  );
};

export default HeaderActionsDropdown;
