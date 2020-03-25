import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import { Button, ButtonToolbar, Col, Row } from 'components/graylog';
import { DocumentTitle, IfPermitted, PageHeader } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import EventDefinitionsContainer from 'components/event-definitions/event-definitions/EventDefinitionsContainer';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';

const EventDefinitionsPage = () => {
  return (
    <DocumentTitle title="Event Definitions">
      <span>
        <PageHeader title="Event Definitions">
          <span>
            Create new Event Definitions that will allow you to search for different Conditions and alert on them.
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
            <IfPermitted permissions="eventdefinitions:read">
              <LinkContainer to={Routes.ALERTS.DEFINITIONS.LIST}>
                <Button bsStyle="info" className="active">Event Definitions</Button>
              </LinkContainer>
            </IfPermitted>
            <IfPermitted permissions="eventnotifications:read">
              <LinkContainer to={Routes.ALERTS.NOTIFICATIONS.LIST}>
                <Button bsStyle="info">Notifications</Button>
              </LinkContainer>
            </IfPermitted>
          </ButtonToolbar>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <EventDefinitionsContainer />
          </Col>
        </Row>
      </span>
    </DocumentTitle>
  );
};

export default EventDefinitionsPage;
