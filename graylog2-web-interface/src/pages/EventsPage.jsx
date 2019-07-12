import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import { Button, ButtonToolbar, Col, Row } from 'react-bootstrap';

import { DocumentTitle, PageHeader } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import EventsContainer from 'components/events/events/EventsContainer';

import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';

class EventsPage extends React.Component {
  static propTypes = {};

  render() {
    return (
      <DocumentTitle title="Events">
        <span>
          <PageHeader title="Events">
            <span>
              Events are generated when Event Definitions you define are satisfied. Alerts also trigger
              Notifications, being meant to capture relevant Events that may require your attention.
            </span>

            <span>
              Graylog&apos;s new Alerting system let you define more flexible and powerful rules. Learn more in the{' '}
              <DocumentationLink page={DocsHelper.PAGES.ALERTS}
                                 text="documentation" />
            </span>

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
              <EventsContainer />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  }
}

export default EventsPage;
