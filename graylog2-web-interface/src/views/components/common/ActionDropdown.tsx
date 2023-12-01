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
import PropTypes from 'prop-types';

import { MenuItem } from 'components/bootstrap';
import Menu from 'components/bootstrap/Menu';

type Props = {
  children: React.ReactNode,
  element: React.ReactNode,
};

const stopEvent = (e: React.BaseSyntheticEvent) => e.stopPropagation();

const ActionDropdown = ({ children, element }: Props) => (
  <span onClick={stopEvent}>
    <Menu position="bottom" withinPortal>
      <Menu.Target>
        {element}
      </Menu.Target>
      <Menu.Dropdown>
        <MenuItem header>Actions</MenuItem>
        {children}
      </Menu.Dropdown>
    </Menu>
  </span>
);

ActionDropdown.propTypes = {
  children: PropTypes.node.isRequired,
  element: PropTypes.node.isRequired,
};

export default ActionDropdown;
