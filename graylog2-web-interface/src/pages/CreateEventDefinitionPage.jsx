import React from 'react';
import PropTypes from 'prop-types';
import { LinkContainer } from 'react-router-bootstrap';

import { ButtonToolbar, Col, Row, Button } from 'components/graylog';
import { DocumentTitle, IfPermitted, PageHeader } from 'components/common';
import EventDefinitionFormContainer
  from 'components/event-definitions/event-definition-form/EventDefinitionFormContainer';
import DocumentationLink from 'components/support/DocumentationLink';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import CombinedProvider from 'injection/CombinedProvider';
import connect from 'stores/connect';
import PermissionsMixin from 'util/PermissionsMixin';
import history from 'util/History';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

class CreateEventDefinitionPage extends React.Component {
  static propTypes = {
    currentUser: PropTypes.object.isRequired,
    route: PropTypes.object.isRequired,
  };

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

    const { currentUser, route } = this.props;

    if (!PermissionsMixin.isPermitted(currentUser.permissions, 'eventdefinitions:create')) {
      history.push(Routes.NOTFOUND);
    }

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
                <Button bsStyle="info">Alerts & Events</Button>
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
              <EventDefinitionFormContainer action="create"
                                            onEventDefinitionChange={this.handleEventDefinitionChange}
                                            route={route} />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  }
}

export default connect(CreateEventDefinitionPage, {
  currentUser: CurrentUserStore,
},
({ currentUser }) => ({ currentUser: currentUser.currentUser }));
