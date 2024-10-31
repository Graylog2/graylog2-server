/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';

import ErrorsActions from 'actions/errors/ErrorsActions';
import useCurrentUser from 'hooks/useCurrentUser';
import { createFromFetchError } from 'logic/errors/ReportedErrors';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import { isPermitted } from 'util/PermissionsMixin';
import EventNotificationDetails from 'components/event-notifications/event-notification-details/EventNotificationDetails';
import EventNotificationActionLinks from 'components/event-notifications/event-notification-details/EventNotificationActionLinks';
import type { EventNotification } from 'stores/event-notifications/EventNotificationsStore';
import { EventNotificationsActions } from 'stores/event-notifications/EventNotificationsStore';
import EventsPageNavigation from 'components/events/EventsPageNavigation';

import useHistory from '../routing/useHistory';

const ShowEventDefinitionPage = () => {
  const currentUser = useCurrentUser();
  const [notification, setNotification] = useState<EventNotification | undefined>();
  const { notificationId } = useParams();
  const history = useHistory();

  useEffect(() => {
    EventNotificationsActions.get(notificationId).then(
      (result) => setNotification(result),
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
      <EventsPageNavigation />
      <PageHeader title={`View "${notification.title}" Notification`}
                  actions={notification && <EventNotificationActionLinks notificationId={notification.id} />}
                  documentationLink={{
                    title: 'Alerts documentation',
                    path: DocsHelper.PAGES.ALERTS,
                  }}>
        <span>
          Notifications alert you of any configured Event when they occur. Graylog can send Notifications directly
          to you or to other systems you use for that purpose.
        </span>
      </PageHeader>

      <EventNotificationDetails notification={notification} />
    </DocumentTitle>
  );
};

export default ShowEventDefinitionPage;
