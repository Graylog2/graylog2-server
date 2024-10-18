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
import styled from 'styled-components';

import Icon from 'components/common/Icon';

import Menu from './Menu';
import Button from './Button';

const StyledIcon = styled(Icon)`
  width: 10px;
  height: 10px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
`;

/**
 * This is the default dropdown button component. If you need to display the dropdown in a portal, please use the `OverlayDropdownButton`.
 */
type ButtonProps = Omit<React.ComponentProps<typeof Button>, 'title' | 'children'>;
type Props = React.PropsWithChildren<ButtonProps & {
  buttonTitle?: string,
  closeOnItemClick?: boolean,
  dropup?: boolean,
  keepMounted?: boolean,
  noCaret?: boolean,
  onMouseDown?: () => void,
  onToggle?: (isOpen: boolean) => void,
  pullRight?: boolean,
  title?: React.ReactNode,
}>;

const position = (pullRight: boolean, dropup: boolean): 'top' | 'bottom' | 'top-end' | 'bottom-end' => {
  const orientation = dropup ? 'top' : 'bottom';
  const suffix = pullRight ? '-end' : '';

  return `${orientation}${suffix}`;
};

const DropdownButton = ({ buttonTitle, children, closeOnItemClick = true, dropup, title, onMouseDown, onToggle, pullRight, noCaret, keepMounted, ...rest }: Props) => (
  <Menu position={position(pullRight, dropup)} onChange={onToggle} keepMounted={keepMounted} closeOnItemClick={closeOnItemClick}>
    <Menu.Target>
      <Button onClick={onMouseDown} aria-label={buttonTitle} {...rest} title={buttonTitle}>
        {title}{noCaret ? null : <>{' '}<StyledIcon name="arrow_drop_down" /></>}
      </Button>
    </Menu.Target>
    <Menu.Dropdown>{children}</Menu.Dropdown>
  </Menu>
);

/** @component */
export default DropdownButton;
