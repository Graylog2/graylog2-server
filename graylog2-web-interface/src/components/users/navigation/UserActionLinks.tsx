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
import { useTranslation } from 'react-i18next';

import { IfPermitted, LinkContainer } from 'components/common';
import type User from 'logic/users/User';
import Routes from 'routing/Routes';
import { ButtonToolbar, Button } from 'components/bootstrap';

type Props = {
  userId: User['id'];
  userIsReadOnly: boolean;
  username: User['username'];
};

const UserActionLinks = ({ userId, userIsReadOnly, username }: Props) => {
  const { t } = useTranslation('users');

  return (
    <ButtonToolbar>
      <IfPermitted permissions={`users:edit:${username}`}>
        <LinkContainer to={Routes.SYSTEM.USERS.show(userId)}>
          <Button>{t('user_navigation.view_details')}</Button>
        </LinkContainer>
      </IfPermitted>
      {!userIsReadOnly && (
        <IfPermitted permissions={`users:edit:${username}`}>
          <LinkContainer to={Routes.SYSTEM.USERS.edit(userId)}>
            <Button bsStyle="primary">{t('user_navigation.edit_user')}</Button>
          </LinkContainer>
        </IfPermitted>
      )}
      <IfPermitted permissions={[`users:tokenlist:${username}`]}>
        <LinkContainer to={Routes.SYSTEM.USERS.TOKENS.edit(userId)}>
          <Button bsStyle="primary">{t('user_navigation.edit_tokens')}</Button>
        </LinkContainer>
      </IfPermitted>
    </ButtonToolbar>
  );
};

export default UserActionLinks;
