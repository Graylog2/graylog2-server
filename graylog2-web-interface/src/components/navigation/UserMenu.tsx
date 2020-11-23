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
// @flow strict
import PropTypes from 'prop-types';
import React from 'react';

import { LinkContainer } from 'components/graylog/router';
import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';
import { NavDropdown, MenuItem } from 'components/graylog';
import { Icon } from 'components/common';
import Routes from 'routing/Routes';
import history from 'util/History';

import ThemeModeToggle from './ThemeModeToggle';

const SessionStore = StoreProvider.getStore('Session');
const SessionActions = ActionsProvider.getActions('Session');

type Props = {
  fullName: string,
  userId: string,
  readOnly: boolean,
};

const UserMenu = ({ fullName, readOnly = true, userId }: Props) => {
  const route = readOnly
    ? Routes.SYSTEM.USERS.show(userId)
    : Routes.SYSTEM.USERS.edit(userId);
  const label = readOnly
    ? 'Show profile'
    : 'Edit profile';

  const onLogoutClicked = () => {
    SessionActions.logout.triggerPromise(SessionStore.getSessionId()).then(() => {
      history.push(Routes.STARTPAGE);
    });
  };

  return (
    <NavDropdown title={<Icon name="user" size="lg" />}
                 aria-label={fullName}
                 id="user-menu-dropdown"
                 noCaret>
      <MenuItem header>{fullName}</MenuItem>
      <MenuItem divider />
      <MenuItem header>
        <ThemeModeToggle />
      </MenuItem>
      <MenuItem divider />
      <LinkContainer to={route}>
        <MenuItem>{label}</MenuItem>
      </LinkContainer>
      <MenuItem onSelect={onLogoutClicked}><Icon name="sign-out-alt" /> Log out</MenuItem>
    </NavDropdown>
  );
};

UserMenu.propTypes = {
  userId: PropTypes.string.isRequired,
  fullName: PropTypes.string.isRequired,
};

export default UserMenu;
