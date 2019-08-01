import React from 'react';
import PropTypes from 'prop-types';
import { LinkContainer } from 'react-router-bootstrap';
import { Button, ButtonToolbar, Col, Row } from 'react-bootstrap';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import EventDefinitionFormContainer
  from 'components/event-definitions/event-definition-form/EventDefinitionFormContainer';
import DocumentationLink from 'components/support/DocumentationLink';

import CombinedProvider from 'injection/CombinedProvider';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';

const { EventDefinitionsActions } = CombinedProvider.get('EventDefinitions');

class EditEventDefinitionPage extends React.Component {
  static propTypes = {
    params: PropTypes.object.isRequired,
  };

  state = {
    eventDefinition: undefined,
  };

  componentDidMount() {
    const { params } = this.props;
    EventDefinitionsActions.get(params.definitionId)
      .then(eventDefinition => this.setState({ eventDefinition: eventDefinition }));
  }

  render() {
    const { eventDefinition } = this.state;

    if (!eventDefinition) {
      return (
        <DocumentTitle title="Edit Event Definition">
          <span>
            <PageHeader title="Edit Event Definition">
              <Spinner text="Loading Event Definition..." />
            </PageHeader>
          </span>
        </DocumentTitle>
      );
    }

    return (
      <DocumentTitle title={`Edit "${eventDefinition.title}" Event Definition`}>
        <span>
          <PageHeader title={`Edit "${eventDefinition.title}" Event Definition`}>
            <span>
              Event Definitions allow you to create Events from different Conditions and alert on them.
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
              <EventDefinitionFormContainer action="edit" eventDefinition={eventDefinition} />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  }
}

export default EditEventDefinitionPage;
