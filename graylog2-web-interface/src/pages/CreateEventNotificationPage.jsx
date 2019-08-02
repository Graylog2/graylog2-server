import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import { Button, ButtonToolbar, Col, Row } from 'react-bootstrap';

import { DocumentTitle, PageHeader } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';

import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';

import EventNotificationFormContainer from 'components/event-notifications/event-notification-form/EventNotificationFormContainer';

class CreateEventDefinitionPage extends React.Component {
  render() {
    return (
      <DocumentTitle title="New Notification">
        <span>
          <PageHeader title="New Notification">
            <span>
              Notifications alert you of any configured Event when they occur. Graylog can send Notifications directly
              to you or to other systems you use for that purpose.
            </span>

            <span>
              Graylog&apos;s new Alerting system let you define more flexible and powerful rules. Learn more in the{' '}
              <DocumentationLink page={DocsHelper.PAGES.ALERTS}
                                 text="documentation" />
            </span>

            <ButtonToolbar>
              <LinkContainer to={Routes.ALERTS.LIST}>
                <Button bsStyle="info">Alerts & Events</Button>
              </LinkContainer>
              <LinkContainer to={Routes.ALERTS.DEFINITIONS.LIST}>
                <Button bsStyle="info">Event Definitions</Button>
              </LinkContainer>
              <LinkContainer to={Routes.ALERTS.NOTIFICATIONS.LIST}>
                <Button bsStyle="info">Notifications</Button>
              </LinkContainer>
            </ButtonToolbar>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <EventNotificationFormContainer action="create" />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  }
}

export default CreateEventDefinitionPage;
