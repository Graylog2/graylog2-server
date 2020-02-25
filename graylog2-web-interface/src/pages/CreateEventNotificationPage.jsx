import React from 'react';
import PropTypes from 'prop-types';
import { LinkContainer } from 'react-router-bootstrap';

import { ButtonToolbar, Col, Row, Button } from 'components/graylog';
import { DocumentTitle, IfPermitted, PageHeader } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';

import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import CombinedProvider from 'injection/CombinedProvider';
import connect from 'stores/connect';
import PermissionsMixin from 'util/PermissionsMixin';
import history from 'util/History';

import EventNotificationFormContainer from 'components/event-notifications/event-notification-form/EventNotificationFormContainer';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

const CreateEventDefinitionPage = ({ currentUser, route }) => {
  if (!PermissionsMixin.isPermitted(currentUser.permissions, 'eventnotifications:create')) {
    history.push(Routes.NOTFOUND);
  }

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
            <EventNotificationFormContainer action="create" route={route} />
          </Col>
        </Row>
      </span>
    </DocumentTitle>
  );
};

CreateEventDefinitionPage.propTypes = {
  currentUser: PropTypes.object.isRequired,
  route: PropTypes.object.isRequired,
};

export default connect(CreateEventDefinitionPage, {
  currentUser: CurrentUserStore,
},
({ currentUser }) => ({ currentUser: currentUser.currentUser }));
