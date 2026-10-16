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
import { Alert, Row, Col } from 'components/bootstrap';
import { DocumentTitle, PageHeader } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import BackendsOverview from 'components/authentication/BackendsOverview';
import AuthenticationPageNavigation from 'components/authentication/AuthenticationPageNavigation';
import BackendActionLinks from 'components/authentication/BackendActionLinks';
import useActiveBackend from 'components/authentication/useActiveBackend';

const AuthenticationOverviewPage = () => {
  const { t } = useTranslation('authentication_overview');
  const { finishedLoading, activeBackend, backendsTotal } = useActiveBackend();

  return (
    <DocumentTitle title={t('title')}>
      <AuthenticationPageNavigation />
      <PageHeader
        title={t('title')}
        actions={<BackendActionLinks activeBackend={activeBackend} finishedLoading={finishedLoading} />}
        documentationLink={{
          title: t('documentation_title'),
          path: DocsHelper.PAGES.USERS_ROLES,
        }}>
        <span>{t('description')}</span>
      </PageHeader>
      {!!(backendsTotal && backendsTotal >= 1 && !activeBackend) && (
        <Row className="content">
          <Col xs={12}>
            <Alert bsStyle="warning">{t('no_active_backend')}</Alert>
          </Col>
        </Row>
      )}
      <BackendsOverview />
    </DocumentTitle>
  );
};

export default AuthenticationOverviewPage;
