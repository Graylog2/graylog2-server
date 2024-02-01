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
import React from 'react';
import styled from 'styled-components';

import { LinkContainer } from 'components/common/router';
import { NavDropdown } from 'components/bootstrap';
import { Icon } from 'components/common';
import Routes from 'routing/Routes';
import useCurrentUser from 'hooks/useCurrentUser';
import Menu from 'components/bootstrap/Menu';
import useLogout from 'hooks/useLogout';

import ThemeModeToggle from './ThemeModeToggle';

const FullName = styled.span`
  text-transform: uppercase;
  font-weight: 700;
`;

const UserMenu = () => {
  const { fullName, readOnly, id: userId } = useCurrentUser();
  const route = readOnly
    ? Routes.SYSTEM.USERS.show(userId)
    : Routes.SYSTEM.USERS.edit(userId);
  const label = readOnly
    ? 'Show profile'
    : 'Edit profile';

  const onLogoutClicked = useLogout();

  return (
    <NavDropdown title={<Icon name="user" size="lg" />}
                 hoverTitle={`User Menu for ${fullName}`}
                 noCaret>
      <Menu.Label><FullName>{fullName}</FullName></Menu.Label>
      <Menu.Divider />
      <Menu.Label>
        <ThemeModeToggle />
      </Menu.Label>
      <Menu.Divider />
      <LinkContainer to={route}>
        <Menu.Item>{label}</Menu.Item>
      </LinkContainer>
      <Menu.Item onClick={onLogoutClicked} icon={<Icon name="sign-out-alt" />}>Log out</Menu.Item>
    </NavDropdown>
  );
};

export default UserMenu;
