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
import { Link } from 'react-router-dom';

import usePluginEntities from 'hooks/usePluginEntities';
import { NavDropdown } from 'components/bootstrap';
import { Icon } from 'components/common';
import useHotkeysContext from 'hooks/useHotkeysContext';
import Menu from 'components/bootstrap/Menu';
import NavIcon from 'components/navigation/NavIcon';
import usePermissions from 'hooks/usePermissions';

const HelpMenu = () => {
  const { setShowHotkeysModal } = useHotkeysContext();
  const menuItems = usePluginEntities('helpMenu');
  const { isPermitted } = usePermissions();
  const availableMenuItems = menuItems.filter((item) => isPermitted(item.permissions));

  return (
    <NavDropdown title={<NavIcon type="help" />} hoverTitle="Help" noCaret>
      {availableMenuItems.map((item) => {
        if ('externalLink' in item) {
          return (
            <Menu.Item
              key={item.description}
              component="a"
              href={item.externalLink}
              target="_blank"
              leftSection={<Icon name="open_in_new" />}>
              {item.description}
            </Menu.Item>
          );
        }

        if ('path' in item) {
          return (
            <Menu.Item key={item.description} component={Link} to={item.path}>
              {item.description}
            </Menu.Item>
          );
        }

        if ('action' in item) {
          return (
            <Menu.Item
              key={item.description}
              onClick={() => item.action({ showHotkeysModal: () => setShowHotkeysModal(true) })}>
              {item.description}
            </Menu.Item>
          );
        }

        throw Error('Help menu item must have either external link, path, or action defined');
      })}
    </NavDropdown>
  );
};

export default HelpMenu;
