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

import Menu from 'components/bootstrap/Menu';
import Button from 'components/bootstrap/Button';
import { ButtonGroup } from 'components/bootstrap/index';

import Icon from '../common/Icon';

type Props = {
  disabled?: boolean,
  title: React.ComponentProps<typeof Button>['children'],
  open?: boolean,
  onMenuChange?: (newState: boolean) => void,
  width?: number,
} & Pick<React.ComponentProps<typeof Button>, 'bsStyle' | 'bsSize' | 'children' | 'id' | 'onClick'>;
const SplitButton = React.forwardRef<HTMLButtonElement, Props>(
  ({ children, disabled = false, title, open, onMenuChange, width, onClick, ...props }, ref) => (
    <Menu opened={open} onChange={onMenuChange} width={width}>
      <ButtonGroup>
        <Button {...props} disabled={disabled} onClick={onClick}>
          {title}
        </Button>
        <Menu.Target>
          <Button ref={ref} aria-label="More Actions" {...props}><Icon name="arrow_drop_down" /></Button>
        </Menu.Target>
        <Menu.Dropdown>
          {children}
        </Menu.Dropdown>
      </ButtonGroup>
    </Menu>
  ),
);

/** @component */
export default SplitButton;
