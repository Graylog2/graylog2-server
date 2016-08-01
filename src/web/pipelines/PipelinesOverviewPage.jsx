import React from 'react';
import { Row, Col, Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { PageHeader } from 'components/common';
import ProcessingTimelineComponent from './ProcessingTimelineComponent';

import Routes from 'routing/Routes';

const PipelinesOverviewPage = React.createClass({
  render() {
    return (
      <div>
        <PageHeader title="Pipelines overview" experimental>
          <span>
            Pipelines let you transform and process messages coming from streams. Pipelines consist of stages where{' '}
            rules are evaluated and applied. Messages can go through one or more stages.
          </span>
          <span>
            Click on a pipeline name to view more about it and edit its stages.
          </span>

          <span>
            <LinkContainer to={Routes.pluginRoute('SYSTEM_PIPELINES')}>
              <Button bsStyle="info">Manage connections</Button>
            </LinkContainer>
            {' '}
            <LinkContainer to={Routes.pluginRoute('SYSTEM_PIPELINES_RULES')}>
              <Button bsStyle="info">Manage rules</Button>
            </LinkContainer>
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <ProcessingTimelineComponent/>
          </Col>
        </Row>
      </div>
    );
  },
});

export default PipelinesOverviewPage;
