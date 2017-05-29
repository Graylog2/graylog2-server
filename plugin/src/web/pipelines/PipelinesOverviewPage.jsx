import React from 'react';
import { Row, Col, Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { DocumentTitle, PageHeader } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import ProcessingTimelineComponent from './ProcessingTimelineComponent';

import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';

const PipelinesOverviewPage = React.createClass({
  render() {
    return (
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
              <LinkContainer to={Routes.pluginRoute('SYSTEM_PIPELINES')}>
                <Button bsStyle="info active">Manage pipelines</Button>
              </LinkContainer>
              &nbsp;
              <LinkContainer to={Routes.pluginRoute('SYSTEM_PIPELINES_RULES')}>
                <Button bsStyle="info">Manage rules</Button>
              </LinkContainer>
              &nbsp;
              <LinkContainer to={Routes.pluginRoute('SYSTEM_PIPELINES_SIMULATE')}>
                <Button bsStyle="info">Simulator</Button>
              </LinkContainer>
            </span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <ProcessingTimelineComponent/>
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  },
});

export default PipelinesOverviewPage;
