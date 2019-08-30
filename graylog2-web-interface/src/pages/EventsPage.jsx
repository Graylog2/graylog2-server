import React from 'react';
import PropTypes from 'prop-types';
import { LinkContainer } from 'react-router-bootstrap';
import { ButtonToolbar, Col, Row } from 'react-bootstrap';
import { Button } from 'components/graylog';

import { DocumentTitle, IfPermitted, PageHeader } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import EventsContainer from 'components/events/events/EventsContainer';

import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';

class EventsPage extends React.Component {
  static propTypes = {
    location: PropTypes.object.isRequired,
  };

  render() {
    const { location } = this.props;
    const filteredSourceStream = location.query.stream_id;

    return (
      <DocumentTitle title="Alerts & Events">
        <span>
          <PageHeader title="Alerts & Events">
            <span>
              Define Events through different conditions. Add Notifications to Events that require your attention
              to create Alerts.
            </span>

            <span>
              Graylog&apos;s new Alerting system let you define more flexible and powerful rules. Learn more in the{' '}
              <DocumentationLink page={DocsHelper.PAGES.ALERTS}
                                 text="documentation" />
            </span>

            <ButtonToolbar>
              <LinkContainer to={Routes.ALERTS.LIST}>
                <Button bsStyle="info" className="active">Alerts & Events</Button>
              </LinkContainer>
              <IfPermitted permissions="eventdefinitions:read">
                <LinkContainer to={Routes.ALERTS.DEFINITIONS.LIST}>
                  <Button bsStyle="info">Event Definitions</Button>
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
              <EventsContainer key={filteredSourceStream} streamId={filteredSourceStream} />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  }
}

export default EventsPage;
