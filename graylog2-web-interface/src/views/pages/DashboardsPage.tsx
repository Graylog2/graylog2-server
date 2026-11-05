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

import { Col, Row } from 'components/bootstrap';
import { DocumentTitle, PageHeader } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import DashboardsOverview from 'views/components/dashboard/DashboardsOverview';
import CreateButton from 'components/common/CreateButton';
import IfPermitted from 'components/common/IfPermitted';

const DashboardsPage = () => {
  const { t } = useTranslation('search');

  return (
    <DocumentTitle title={t('main.dashboards', { ns: 'navigation' })}>
      <PageHeader
        title={t('main.dashboards', { ns: 'navigation' })}
        actions={
          <IfPermitted permissions={['dashboards:create']}>
            <CreateButton entityKey="Dashboard" />
          </IfPermitted>
        }
        documentationLink={{
          title: t('dashboards_page.documentation'),
          path: DocsHelper.PAGES.DASHBOARDS,
        }}>
        <span>{t('dashboards_page.description')}</span>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          <DashboardsOverview />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default DashboardsPage;
