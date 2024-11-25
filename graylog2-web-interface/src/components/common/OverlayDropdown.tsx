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
import React, { useRef } from 'react';
import styled, { css } from 'styled-components';

import Menu from 'components/bootstrap/Menu';

type Placement = 'top' | 'right' | 'bottom' | 'left';

const ToggleDropdown = styled.span<{ $alwaysShowCaret: boolean }>(({ $alwaysShowCaret }) => css`
  cursor: pointer;

  ${$alwaysShowCaret ? '' : css`
    .caret {
      visibility: hidden;
    }

    &:hover .caret {
      visibility: visible;
    }
  `}
`);

type Props = {
  alwaysShowCaret?: boolean,
  children: React.ReactNode,
  closeOnSelect?: boolean,
  dropdownZIndex?: number,
  menuContainer?: HTMLElement,
  onToggle: () => void,
  placement?: Placement,
  show: boolean,
  toggleChild?: React.ReactNode,
}

const OverlayDropdown = ({
  alwaysShowCaret = false,
  children,
  closeOnSelect = true,
  dropdownZIndex,
  menuContainer = document.body,
  onToggle,
  placement = 'bottom',
  show,
  toggleChild = 'Toggle',
}: Props) => {
  const toggleTarget = useRef<HTMLButtonElement>();

  return (
    <Menu opened={show}
          withinPortal
          position={placement}
          closeOnItemClick={closeOnSelect}
          onClose={onToggle}
          portalProps={{ target: menuContainer }}
          zIndex={dropdownZIndex}>
      <Menu.Target>
        <ToggleDropdown $alwaysShowCaret={alwaysShowCaret}
                        onClick={onToggle}
                        ref={toggleTarget}
                        role="presentation">
          {toggleChild}
        </ToggleDropdown>
      </Menu.Target>
      <Menu.Dropdown>
        {children}
      </Menu.Dropdown>
    </Menu>
  );
};

export default OverlayDropdown;
