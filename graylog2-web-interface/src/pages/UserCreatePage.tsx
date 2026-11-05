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

import { LinkContainer, PageHeader, DocumentTitle } from 'components/common';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import { Button } from 'components/bootstrap';
import UserCreate from 'components/users/UserCreate';
import UsersPageNavigation from 'components/users/navigation/UsersPageNavigation';

const UserCreatePage = () => {
  const { t } = useTranslation('users');

  return (
    <DocumentTitle title={t('user_create.title')}>
      <UsersPageNavigation />
      <PageHeader
        title={t('user_create.title')}
        actions={
          <LinkContainer to={Routes.SYSTEM.USERS.CREATE}>
            <Button bsStyle="primary">{t('user_create.submit_button')}</Button>
          </LinkContainer>
        }
        documentationLink={{
          title: t('user_create.documentation_link_title'),
          path: DocsHelper.PAGES.USERS_ROLES,
        }}>
        <span>{t('user_create.description')}</span>
      </PageHeader>

      <UserCreate />
    </DocumentTitle>
  );
};

export default UserCreatePage;
