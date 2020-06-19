import React from 'react';
import createReactClass from 'create-react-class';
import { LinkContainer } from 'react-router-bootstrap';

import { ButtonToolbar, Col, Row, Button } from 'components/graylog';
import { DocumentTitle, PageHeader } from 'components/common';
import Routes from 'routing/Routes';
import CollectorForm from 'components/sidecars/configuration-forms/CollectorForm';

const SidecarNewCollectorPage = createReactClass({
  displayName: 'SidecarNewCollectorPage',

  render() {
    return (
      <DocumentTitle title="New Log Collector">
        <span>
          <PageHeader title="New Log Collector">
            <span>
              Some words about log collectors.
            </span>

            <span>
              Read more about the Graylog Sidecar in the documentation.
            </span>

            <ButtonToolbar>
              <LinkContainer to={Routes.SYSTEM.SIDECARS.OVERVIEW}>
                <Button bsStyle="info">Overview</Button>
              </LinkContainer>
              <LinkContainer to={Routes.SYSTEM.SIDECARS.ADMINISTRATION}>
                <Button bsStyle="info">Administration</Button>
              </LinkContainer>
              <LinkContainer to={Routes.SYSTEM.SIDECARS.CONFIGURATION}>
                <Button bsStyle="info" className="active">Configuration</Button>
              </LinkContainer>
            </ButtonToolbar>
          </PageHeader>

          <Row className="content">
            <Col md={6}>
              <CollectorForm action="create" />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  },
});

export default SidecarNewCollectorPage;
