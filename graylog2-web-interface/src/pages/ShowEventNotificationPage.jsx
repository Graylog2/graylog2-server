import * as React from 'react';
import { useContext, useState, useEffect } from 'react';
import PropTypes from 'prop-types';

import ErrorsActions from 'actions/errors/ErrorsActions';
import CurrentUserContext from 'contexts/CurrentUserContext';
import { LinkContainer } from 'components/graylog/router';
import { ButtonToolbar, Button } from 'components/graylog';
import { createFromFetchError } from 'logic/errors/ReportedErrors';
import { DocumentTitle, IfPermitted, PageHeader, Spinner } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import CombinedProvider from 'injection/CombinedProvider';
import { isPermitted } from 'util/PermissionsMixin';
import history from 'util/History';
import withParams from 'routing/withParams';
import EventNotificationDetails from 'components/event-notifications/event-notification-details/EventNotificationDetails';
import EventNotificationActionLinks from 'components/event-notifications/event-notification-details/EventNotificationActionLinks';

import {} from 'components/event-notifications/event-notification-types';

const { EventNotificationsActions } = CombinedProvider.get('EventNotifications');

const ShowEventDefinitionPage = ({ params: { notificationId } }) => {
  const currentUser = useContext(CurrentUserContext) || {};
  const [notification, setNotification] = useState();

  useEffect(() => {
    EventNotificationsActions.get(notificationId).then(
      setNotification,
      (error) => {
        if (error.status === 404) {
          ErrorsActions.report(createFromFetchError(error));
        }
      },
    );
  }, [notificationId]);

  if (!isPermitted(currentUser.permissions, `eventnotifications:read:${notificationId}`)) {
    history.push(Routes.NOTFOUND);
  }

  if (!notification) {
    return (
      <DocumentTitle title="Notification Details">
        <span>
          <PageHeader title="Notification Details">
            <Spinner text="Loading Notification information..." />
          </PageHeader>
        </span>
      </DocumentTitle>
    );
  }

  return (
    <DocumentTitle title={`View "${notification.title}" Notification`}>
      <span>
        <PageHeader title={`View "${notification.title}" Notification`} subactions={notification && <EventNotificationActionLinks notificationId={notification.id} />}>
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

        <EventNotificationDetails notification={notification} />

      </span>
    </DocumentTitle>
  );
};

ShowEventDefinitionPage.propTypes = {
  params: PropTypes.object.isRequired,
};

export default withParams(ShowEventDefinitionPage);
