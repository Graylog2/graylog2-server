import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import { Button, ButtonToolbar, Col, Row } from 'react-bootstrap';

import { DocumentTitle, PageHeader } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import EventDefinitionsContainer from 'components/event-definitions/event-definitions/EventDefinitionsContainer';

import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';

class EventDefinitionsPage extends React.Component {
  render() {
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
              <LinkContainer to={Routes.NEXT_ALERTS.LIST}>
                <Button bsStyle="info">Events</Button>
              </LinkContainer>
              <LinkContainer to={Routes.NEXT_ALERTS.DEFINITIONS.LIST}>
                <Button bsStyle="info" className="active">Event Definitions</Button>
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
                  <EventDefinitionsContainer />
                </Col>
              </Row>
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  }
}

export default EventDefinitionsPage;
