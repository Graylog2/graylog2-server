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
import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Col, Row } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import usePermissions from 'hooks/usePermissions';
import EventNotificationFormContainer from 'components/event-notifications/event-notification-form/EventNotificationFormContainer';
import EventNotificationActionLinks from 'components/event-notifications/event-notification-details/EventNotificationActionLinks';
import useParams from 'routing/useParams';
import useHistory from 'routing/useHistory';
import { getEventNotification } from 'components/event-notifications/hooks/useEventNotifications';
import EventsPageNavigation from 'components/events/EventsPageNavigation';
import PageDescription from 'components/event-notifications/PageDescription';

const EditEventNotificationPage = () => {
  const { t } = useTranslation('event_notifications');
  const { notificationId } = useParams<{ notificationId: string }>();
  const history = useHistory();
  const { isPermitted } = usePermissions();
  const [notification, setNotification] = useState(undefined);
  const canEdit = isPermitted(`eventnotifications:edit:${notificationId}`);

  useEffect(() => {
    if (!canEdit) {
      history.push(Routes.NOTFOUND);

      return;
    }

    getEventNotification(notificationId).then(
      (result) => setNotification(result),
      (error) => {
        if (error.status === 404) {
          history.push(Routes.ALERTS.NOTIFICATIONS.LIST);
        }
      },
    );
  }, [canEdit, history, notificationId]);

  if (!notification) {
    return (
      <DocumentTitle title={t('pages.edit')}>
        <span>
          <PageHeader title={t('pages.edit')}>
            <Spinner text={t('pages.loading')} />
          </PageHeader>
        </span>
      </DocumentTitle>
    );
  }

  return (
    <DocumentTitle title={t('pages.edit_named', { title: notification.title })}>
      <EventsPageNavigation />
      <PageHeader
        title={t('pages.edit_named', { title: notification.title })}
        actions={<EventNotificationActionLinks notificationId={notification.id} />}
        documentationLink={{
          title: t('pages.alerts_docs'),
          path: DocsHelper.PAGES.ALERTS,
        }}>
        <PageDescription />
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          <EventNotificationFormContainer action="edit" notification={notification} />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default EditEventNotificationPage;
