import React from 'react';

import { LinkContainer } from 'components/graylog/router';
import { Row, Col, Button } from 'components/graylog';
import { DocumentTitle, PageHeader } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import ProcessingTimelineComponent from 'components/pipelines/ProcessingTimelineComponent';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';

const PipelinesOverviewPage = () => (
  <DocumentTitle title="Pipelines">
    <div>
      <PageHeader title="Pipelines overview">
        <span>
          Pipelines let you transform and process messages coming from streams. Pipelines consist of stages where
          rules are evaluated and applied. Messages can go through one or more stages.
        </span>
        <span>
          Read more about Graylog pipelines in the <DocumentationLink page={DocsHelper.PAGES.PIPELINES} text="documentation" />.
        </span>

        <span>
          <LinkContainer to={Routes.SYSTEM.PIPELINES.OVERVIEW}>
            <Button bsStyle="info">Manage pipelines</Button>
          </LinkContainer>
              &nbsp;
          <LinkContainer to={Routes.SYSTEM.PIPELINES.RULES}>
            <Button bsStyle="info">Manage rules</Button>
          </LinkContainer>
              &nbsp;
          <LinkContainer to={Routes.SYSTEM.PIPELINES.SIMULATOR}>
            <Button bsStyle="info">Simulator</Button>
          </LinkContainer>
        </span>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          <ProcessingTimelineComponent />
        </Col>
      </Row>
    </div>
  </DocumentTitle>
);

export default PipelinesOverviewPage;
