import React from 'react';
import PropTypes from 'prop-types';
import { LinkContainer } from 'react-router-bootstrap';

import { ButtonToolbar, Col, Row, Button } from 'components/graylog';
import { DocumentTitle, IfPermitted, PageHeader, Spinner } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';

import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import CombinedProvider from 'injection/CombinedProvider';
import connect from 'stores/connect';
import PermissionsMixin from 'util/PermissionsMixin';
import history from 'util/History';

import EventNotificationFormContainer from 'components/event-notifications/event-notification-form/EventNotificationFormContainer';

const { EventNotificationsActions } = CombinedProvider.get('EventNotifications');
const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

const { isPermitted } = PermissionsMixin;

class EditEventDefinitionPage extends React.Component {
  static propTypes = {
    params: PropTypes.object.isRequired,
    currentUser: PropTypes.object.isRequired,
    route: PropTypes.object.isRequired,
  };

  state = {
    notification: undefined,
  };

  componentDidMount() {
    const { params, currentUser } = this.props;

    if (isPermitted(currentUser.permissions, `eventnotifications:edit:${params.definitionId}`)) {
      EventNotificationsActions.get(params.notificationId)
        .then(
          (notification) => this.setState({ notification: notification }),
          (error) => {
            if (error.status === 404) {
              history.push(Routes.ALERTS.NOTIFICATIONS.LIST);
            }
          },
        );
    }
  }

  render() {
    const { notification } = this.state;
    const { params, currentUser, route } = this.props;

    if (!isPermitted(currentUser.permissions, `eventnotifications:edit:${params.definitionId}`)) {
      history.push(Routes.NOTFOUND);
    }

    if (!notification) {
      return (
        <DocumentTitle title="Edit Notification">
          <span>
            <PageHeader title="Edit Notification">
              <Spinner text="Loading Notification information..." />
            </PageHeader>
          </span>
        </DocumentTitle>
      );
    }

    return (
      <DocumentTitle title={`Edit "${notification.title}" Notification`}>
        <span>
          <PageHeader title={`Edit "${notification.title}" Notification`}>
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
              <EventNotificationFormContainer action="edit" notification={notification} route={route} />
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
