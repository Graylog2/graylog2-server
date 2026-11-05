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

import 'components/authentication/bindings'; // Bind all authentication plugins
import AuthenticationPageNavigation from 'components/authentication/AuthenticationPageNavigation';
import GettingStarted from 'components/authentication/BackendCreate/GettingStarted';
import { DocumentTitle, PageHeader } from 'components/common';
import useActiveBackend from 'components/authentication/useActiveBackend';
import DocsHelper from 'util/DocsHelper';
import BackendActionLinks from 'components/authentication/BackendActionLinks';

const AuthenticationCreatePage = () => {
  const { t } = useTranslation('authentication');
  const { finishedLoading, activeBackend } = useActiveBackend();

  return (
    <DocumentTitle title={t('create_page.title')}>
      <AuthenticationPageNavigation />
      <PageHeader
        title={t('create_page.title')}
        actions={<BackendActionLinks activeBackend={activeBackend} finishedLoading={finishedLoading} />}
        documentationLink={{
          title: t('page.documentation_title'),
          path: DocsHelper.PAGES.USERS_ROLES,
        }}>
        <span>{t('page.description')}</span>
      </PageHeader>

      <GettingStarted title={t('create_page.getting_started_title')} />
    </DocumentTitle>
  );
};

export default AuthenticationCreatePage;
