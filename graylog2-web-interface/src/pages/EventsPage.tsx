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
import { useTranslation } from 'react-i18next';

import { DocumentTitle, PageHeader } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import usePluginEntities from 'hooks/usePluginEntities';
import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';
import EventsPageNavigation from 'components/events/EventsPageNavigation';
import EventsEntityTable from 'components/events/EventsEntityTable';
import { Row, Col } from 'components/bootstrap';

const AlertsOverview = () => {
  const {
    data: { valid: validSecurityLicense },
  } = usePluggableLicenseCheck('/license/security');
  const pluggableSecurityEventsPage = usePluginEntities('views.components.securityEventsPage');

  if (!validSecurityLicense) {
    return <EventsEntityTable />;
  }

  return (
    <>
      {pluggableSecurityEventsPage.map(({ component: PluggableSecurityEventsPage, key }) => (
        <PluggableSecurityEventsPage key={key} />
      ))}
    </>
  );
};

const EventsPage = () => {
  const { t } = useTranslation('events');

  return (
    <DocumentTitle title={t('alerts_page.title')}>
      <EventsPageNavigation />
      <PageHeader
        title={t('alerts_page.title')}
        documentationLink={{
          title: t('alerts_page.documentation_title'),
          path: DocsHelper.PAGES.ALERTS,
        }}>
        <span>{t('alerts_page.description')}</span>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          <AlertsOverview />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default EventsPage;
