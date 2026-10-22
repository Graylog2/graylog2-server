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

import DocsHelper from 'util/DocsHelper';
import { PageHeader, DocumentTitle } from 'components/common';
import UsersOverview from 'components/users/UsersOverview';
import UsersPageNavigation from 'components/users/navigation/UsersPageNavigation';
import CreateButton from 'components/common/CreateButton';

const UsersOverviewPage = () => {
  const { t } = useTranslation('users');

  return (
    <DocumentTitle title={t('users_overview.title')}>
      <UsersPageNavigation />
      <PageHeader
        title={t('users_overview.title')}
        actions={<CreateButton entityKey="User" />}
        documentationLink={{
          title: t('users_overview.documentation_link_title'),
          path: DocsHelper.PAGES.USERS_ROLES,
        }}>
        <span>{t('users_overview.description')}</span>
      </PageHeader>

      <UsersOverview />
    </DocumentTitle>
  );
};

export default UsersOverviewPage;
