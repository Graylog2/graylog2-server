import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import { Button, ButtonToolbar, Col, Row } from 'react-bootstrap';

import { DocumentTitle, PageHeader } from 'components/common';

import EventNotificationsContainer from 'components/event-notifications/event-notifications/EventNotificationsContainer';

import Routes from 'routing/Routes';

class EventNotificationsPage extends React.Component {
  static propTypes = {};

  render() {
    return (
      <DocumentTitle title="Notifications">
        <span>
          <PageHeader title="Notifications">
            <span>
              Notifications alert you of any configured Event when they occur. Graylog can send Notifications directly
              to you or to other systems you use for that purpose.
            </span>

            <span>
              Remember to assign Notifications while creating or editing an Event Definition.
            </span>

            <ButtonToolbar>
              <LinkContainer to={Routes.ALERTS.LIST}>
                <Button bsStyle="info">Alerts & Events</Button>
              </LinkContainer>
              <LinkContainer to={Routes.ALERTS.DEFINITIONS.LIST}>
                <Button bsStyle="info">Event Definitions</Button>
              </LinkContainer>
              <LinkContainer to={Routes.ALERTS.NOTIFICATIONS.LIST}>
                <Button bsStyle="info" className="active">Notifications</Button>
              </LinkContainer>
            </ButtonToolbar>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <EventNotificationsContainer />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  }
}

export default EventNotificationsPage;
