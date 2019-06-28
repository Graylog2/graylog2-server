import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import { Button, ButtonToolbar, Col, Row } from 'react-bootstrap';

import { DocumentTitle, PageHeader } from 'components/common';

import Routes from 'routing/Routes';

class EventsPage extends React.Component {
  static propTypes = {};

  render() {
    return (
      <DocumentTitle title="Events">
        <span>
          <PageHeader title="Events">
            <span />

            <span />

            <ButtonToolbar>
              <LinkContainer to={Routes.NEXT_ALERTS.LIST}>
                <Button bsStyle="info" className="active">Events</Button>
              </LinkContainer>
              <LinkContainer to={Routes.NEXT_ALERTS.DEFINITIONS.LIST}>
                <Button bsStyle="info">Event Definitions</Button>
              </LinkContainer>
              <LinkContainer to={Routes.NEXT_ALERTS.NOTIFICATIONS.LIST}>
                <Button bsStyle="info">Notifications</Button>
              </LinkContainer>
            </ButtonToolbar>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <Row>
                <Col md={12}>
                  TBD
                </Col>
              </Row>
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  }
}

export default EventsPage;
