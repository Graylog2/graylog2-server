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

import { Row, Col, ButtonToolbar } from 'components/bootstrap';
import { DocumentTitle, PageHeader } from 'components/common';
import ProcessingTimelineComponent from 'components/pipelines/ProcessingTimelineComponent';
import DocsHelper from 'util/DocsHelper';
import PipelinesPageNavigation from 'components/pipelines/PipelinesPageNavigation';
import CreateButton from 'components/common/CreateButton';
import {
  EnableDebugMetricsButton,
  ProcessingLoadDebugMetricsBanner,
  ProcessingLoadProvider,
} from 'components/pipelines/processing-load';

const PipelinesOverviewPage = () => {
  const { t } = useTranslation('pipelines');

  return (
    <DocumentTitle title={t('title')}>
      <PipelinesPageNavigation />
      <PageHeader
        title={t('overview_header_title')}
        actions={
          <ButtonToolbar>
            <EnableDebugMetricsButton />
            <CreateButton entityKey="Pipeline" />
          </ButtonToolbar>
        }
        documentationLink={{
          title: t('documentation_link_title'),
          path: DocsHelper.PAGES.PIPELINES,
        }}>
        <span
          >{t('overview_description')}</span>
      </PageHeader>
      <Row className="content">
        <Col md={12}>
          <ProcessingLoadProvider>
            <ProcessingLoadDebugMetricsBanner />
            <ProcessingTimelineComponent />
          </ProcessingLoadProvider>
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default PipelinesOverviewPage;
