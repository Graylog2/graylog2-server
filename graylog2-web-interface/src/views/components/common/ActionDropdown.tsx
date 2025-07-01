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
import { useState } from 'react';

import { MenuItem } from 'components/bootstrap';
import Menu from 'components/bootstrap/Menu';
import StopPropagation from 'views/components/common/StopPropagation';
import { widgetActionsDropdownOpenClass } from 'views/components/widgets/Constants';

type Props = {
  children: React.ReactNode;
  element: React.ReactNode;
  'data-testid'?: string;
  header?: string;
};

const ActionDropdown = ({ children, element, 'data-testid': dataTestid = undefined, header = 'Actions' }: Props) => {
  const [isOpen, setIsOpen] = useState(false);

  const toggleIsOpen = (newIsOpen) => setIsOpen(newIsOpen);

  return (
    <StopPropagation data-testid={dataTestid}>
      <span className={isOpen ? widgetActionsDropdownOpenClass : ''}>
        <Menu position="bottom" withinPortal zIndex={1051} onChange={toggleIsOpen}>
          <Menu.Target>{element}</Menu.Target>
          <Menu.Dropdown>
            <MenuItem header>{header}</MenuItem>
            {children}
          </Menu.Dropdown>
        </Menu>
      </span>
    </StopPropagation>
  );
};

export default ActionDropdown;
