import React from 'react';
import PropTypes from 'prop-types';
import { LinkContainer } from 'react-router-bootstrap';

import { ButtonToolbar, Col, Row, Button } from 'components/graylog';
import { DocumentTitle, IfPermitted, PageHeader, Spinner } from 'components/common';
import EventDefinitionFormContainer
  from 'components/event-definitions/event-definition-form/EventDefinitionFormContainer';
import DocumentationLink from 'components/support/DocumentationLink';

import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import PermissionsMixin from 'util/PermissionsMixin';
import history from 'util/History';

const { EventDefinitionsActions } = CombinedProvider.get('EventDefinitions');
const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

const { isPermitted } = PermissionsMixin;

class EditEventDefinitionPage extends React.Component {
  static propTypes = {
    params: PropTypes.object.isRequired,
    currentUser: PropTypes.object.isRequired,
    route: PropTypes.object.isRequired,
  };

  state = {
    eventDefinition: undefined,
  };

  componentDidMount() {
    const { params, currentUser } = this.props;

    if (isPermitted(currentUser.permissions, `eventdefinitions:edit:${params.definitionId}`)) {
      EventDefinitionsActions.get(params.definitionId)
        .then(
          (eventDefinition) => this.setState({ eventDefinition: eventDefinition }),
          (error) => {
            if (error.status === 404) {
              history.push(Routes.ALERTS.DEFINITIONS.LIST);
            }
          },
        );
    }
  }

  render() {
    const { params, currentUser, route } = this.props;
    const { eventDefinition } = this.state;

    if (!isPermitted(currentUser.permissions, `eventdefinitions:edit:${params.definitionId}`)) {
      history.push(Routes.NOTFOUND);
    }

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
              <EventDefinitionFormContainer action="edit" eventDefinition={eventDefinition} route={route} />
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  }
}

export default connect(EditEventDefinitionPage, {
  currentUser: CurrentUserStore,
},
({ currentUser }) => ({ currentUser: currentUser.currentUser }));
