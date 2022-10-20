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

import { Row, Col } from 'components/bootstrap';
import { DocumentTitle, PageHeader } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import ProcessingTimelineComponent from 'components/pipelines/ProcessingTimelineComponent';
import DocsHelper from 'util/DocsHelper';
import CreatePipelineButton from 'components/pipelines/CreatePipelineButton';
import PipelinesPageNavigation from 'components/pipelines/PipelinesPageNavigation';

const PipelinesOverviewPage = () => (
  <DocumentTitle title="Pipelines">
    <PipelinesPageNavigation />
    <PageHeader title="Pipelines overview"
                subactions={(<CreatePipelineButton />)}>
      <span>
        Pipelines let you transform and process messages coming from streams. Pipelines consist of stages where
        rules are evaluated and applied. Messages can go through one or more stages.
      </span>
      <span>
        Read more about Graylog pipelines in the <DocumentationLink page={DocsHelper.PAGES.PIPELINES} text="documentation" />.
      </span>
    </PageHeader>

    <Row className="content">
      <Col md={12}>
        <ProcessingTimelineComponent />
      </Col>
    </Row>
  </DocumentTitle>
);

export default PipelinesOverviewPage;
