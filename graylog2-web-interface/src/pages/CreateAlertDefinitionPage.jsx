import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import { Button, ButtonToolbar, Col, Row } from 'react-bootstrap';

import Routes from 'routing/Routes';
import { DocumentTitle, PageHeader } from 'components/common';
import AlertDefinitionFormContainer from 'components/alert-definitions/alert-definition-form/AlertDefinitionFormContainer';

class CreateAlertDefinitionPage extends React.Component {
  render() {
    return (
      <DocumentTitle title="New Event Definition">
        <span>
          <PageHeader title="New Event Definition">
            <span>
              Event Definitions allow you to create Events from different Conditions and execute Actions on them.
            </span>

            <span>
              Events are our new alerting system that let you define more flexible rules and
            </span>

            <ButtonToolbar>
              <LinkContainer to={Routes.NEXT_ALERTS.DEFINITIONS.CREATE}>
                <Button bsStyle="success" className="active">Create Alert Definition</Button>
              </LinkContainer>
              <LinkContainer to={Routes.NEXT_ALERTS.DEFINITIONS.LIST}>
                <Button bsStyle="info">Alert Definitions</Button>
              </LinkContainer>
            </ButtonToolbar>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <AlertDefinitionFormContainer action="create" />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  }
}

export default CreateAlertDefinitionPage;
