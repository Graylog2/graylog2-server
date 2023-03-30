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
import PropTypes from 'prop-types';
import React from 'react';

import { LinkContainer } from 'components/common/router';
import { NavDropdown, MenuItem } from 'components/bootstrap';
import { Icon } from 'components/common';
import Routes from 'routing/Routes';
import { SessionActions } from 'stores/sessions/SessionStore';
import useHistory from 'routing/useHistory';

import ThemeModeToggle from './ThemeModeToggle';

type Props = {
  fullName: string,
  userId: string,
  readOnly: boolean,
};

const UserMenu = ({ fullName, readOnly = true, userId }: Props) => {
  const history = useHistory();
  const route = readOnly
    ? Routes.SYSTEM.USERS.show(userId)
    : Routes.SYSTEM.USERS.edit(userId);
  const label = readOnly
    ? 'Show profile'
    : 'Edit profile';

  const onLogoutClicked = () => {
    SessionActions.logout().then(() => {
      /* In some cases, when the authentication info is set externally (e.g. trusted headers), we need to retrigger a
         session validation, so we are not stuck at the login screen. */
      SessionActions.validate();
      history.push(Routes.STARTPAGE);
    });
  };

  return (
    <NavDropdown title={<Icon name="user" size="lg" />}
                 aria-label={`User Menu for ${fullName}`}
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
      <MenuItem onSelect={onLogoutClicked} icon="sign-out-alt">Log out</MenuItem>
    </NavDropdown>
  );
};

UserMenu.propTypes = {
  userId: PropTypes.string.isRequired,
  fullName: PropTypes.string.isRequired,
};

export default UserMenu;
