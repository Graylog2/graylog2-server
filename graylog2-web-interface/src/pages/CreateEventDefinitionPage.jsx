import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import { Button, ButtonToolbar, Col, Row } from 'react-bootstrap';

import { DocumentTitle, PageHeader } from 'components/common';
import EventDefinitionFormContainer
  from 'components/event-definitions/event-definition-form/EventDefinitionFormContainer';
import DocumentationLink from 'components/support/DocumentationLink';

import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';

class CreateEventDefinitionPage extends React.Component {
  state = {
    eventDefinitionTitle: undefined,
  };

  handleEventDefinitionChange = (eventDefinition) => {
    const { eventDefinitionTitle } = this.state;
    if (eventDefinition.title !== eventDefinitionTitle) {
      this.setState({ eventDefinitionTitle: eventDefinition.title });
    }
  };

  render() {
    const { eventDefinitionTitle } = this.state;
    const pageTitle = eventDefinitionTitle ? `New Event Definition "${eventDefinitionTitle}"` : 'New Event Definition';

    return (
      <DocumentTitle title={pageTitle}>
        <span>
          <PageHeader title={pageTitle}>
            <span>
              Event Definitions allow you to create Alerts from different Conditions and alert on them.
            </span>

            <span>
              Graylog&apos;s new Alerting system let you define more flexible and powerful rules. Learn more in the{' '}
              <DocumentationLink page={DocsHelper.PAGES.ALERTS}
                                 text="documentation" />
            </span>

            <ButtonToolbar>
              <LinkContainer to={Routes.ALERTS.LIST}>
                <Button bsStyle="info">Events</Button>
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
              <EventDefinitionFormContainer action="create" onEventDefinitionChange={this.handleEventDefinitionChange} />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  }
}

export default CreateEventDefinitionPage;
