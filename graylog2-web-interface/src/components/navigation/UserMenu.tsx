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
import React, { useContext } from 'react';
import { useTranslation } from 'react-i18next';
import styled, { css } from 'styled-components';

import { Icon, LinkContainer } from 'components/common';
import { NavDropdown, MenuItem } from 'components/bootstrap';
import Routes from 'routing/Routes';
import useCurrentUser from 'hooks/useCurrentUser';
import Menu from 'components/bootstrap/Menu';
import useLogout from 'hooks/useLogout';
import NavIcon from 'components/navigation/NavIcon';
import UserPreferencesContext from 'contexts/UserPreferencesContext';
import { getLanguage } from 'components/users/languages/Languages';
import LanguageLabel from 'components/users/languages/LanguageLabel';

import ThemeModeToggle from './ThemeModeToggle';

const FullName = styled.span`
  text-transform: uppercase;
  font-weight: 700;
`;

const LanguageRow = styled.span(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.xs};
    min-width: 220px;
  `,
);

const CurrentLanguage = styled.span(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.xxs};
    margin-left: auto;
    color: ${theme.colors.text.secondary};
  `,
);

const UserMenu = () => {
  const { fullName, readOnly, id: userId } = useCurrentUser();
  const { language } = useContext(UserPreferencesContext);
  const route = readOnly ? Routes.SYSTEM.USERS.show(userId) : Routes.SYSTEM.USERS.edit(userId);
  const { t } = useTranslation('navigation');
  const label = readOnly ? t('user_menu.show_profile') : t('user_menu.edit_profile');

  const onLogoutClicked = useLogout();

  return (
    <NavDropdown title={<NavIcon type="user_menu" />} hoverTitle={t('user_menu.title', { name: fullName })} noCaret>
      <Menu.Label>
        <FullName>{fullName}</FullName>
      </Menu.Label>
      <MenuItem divider />
      <Menu.Label>
        <ThemeModeToggle />
      </Menu.Label>
      <MenuItem divider />
      <LinkContainer to={route}>
        <MenuItem icon="translate">
          <LanguageRow>
            {t('user_menu.language')}
            <CurrentLanguage>
              <LanguageLabel language={getLanguage(language)} />
              <Icon name="chevron_right" />
            </CurrentLanguage>
          </LanguageRow>
        </MenuItem>
      </LinkContainer>
      <MenuItem divider />
      <LinkContainer to={route}>
        <MenuItem>{label}</MenuItem>
      </LinkContainer>
      <MenuItem onClick={onLogoutClicked} icon="logout">
        {t('user_menu.log_out')}
      </MenuItem>
    </NavDropdown>
  );
};

export default UserMenu;
