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

import { Col, Row } from 'components/bootstrap';
import { DocumentTitle, PageHeader } from 'components/common';
import EventDefinitionsContainer from 'components/event-definitions/event-definitions/EventDefinitionsContainer';
import DocsHelper from 'util/DocsHelper';
import EventsPageNavigation from 'components/events/EventsPageNavigation';
import CreateButton from 'components/common/CreateButton';
import usePluginEntities from 'hooks/usePluginEntities';

const CreateButtonWithSigma = () => {
  const importOptionsPlugin = usePluginEntities('eventDefinitions.components.sigmaOptions');
  const SigmaOptionsComponent = importOptionsPlugin.find(
    ({ key }) => key === 'eventDefinitionsSigmaOptions',
  )?.component;

  return (
    <>
      <CreateButton entityKey="Event Definition" />
      {SigmaOptionsComponent ? <SigmaOptionsComponent /> : null}
    </>
  );
};

const EventDefinitionsPage = () => {
  const { t } = useTranslation('events');

  return (
    <DocumentTitle title={t('definitions_page.title')}>
      <EventsPageNavigation />
      <PageHeader
        title={t('definitions_page.title')}
        actions={<CreateButtonWithSigma />}
        documentationLink={{
          title: t('definitions_page.alerts_docs'),
          path: DocsHelper.PAGES.ALERTS,
        }}>
        <span>{t('definitions_page.description_list')}</span>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          <EventDefinitionsContainer />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default EventDefinitionsPage;
