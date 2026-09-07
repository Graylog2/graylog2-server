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

import { Col, Row } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import ProcessorSimulator from 'components/simulator/ProcessorSimulator';
import DocsHelper from 'util/DocsHelper';
import PipelinesPageNavigation from 'components/pipelines/PipelinesPageNavigation';
import useEditableStreams from 'hooks/useEditableStreams';

const SimulatorPage = () => {
  const streams = useEditableStreams();

  const content = !streams ? <Spinner /> : <ProcessorSimulator streams={streams} />;

  return (
    <DocumentTitle title="Simulate processing">
      <PipelinesPageNavigation />
      <PageHeader
        title="Simulate processing"
        documentationLink={{
          title: 'Pipelines documentation',
          path: DocsHelper.PAGES.PIPELINE_RULES,
        }}>
        <span>
          Processing messages can be complex. Use this page to simulate the result of processing an incoming message
          using your current set of pipelines and rules.
        </span>
      </PageHeader>

      <Row className="content">
        <Col md={12}>{content}</Col>
      </Row>
    </DocumentTitle>
  );
};

export default SimulatorPage;
