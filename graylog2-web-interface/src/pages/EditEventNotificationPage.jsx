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
import React from 'react';
import PropTypes from 'prop-types';

import { Col, Row } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import connect from 'stores/connect';
import PermissionsMixin from 'util/PermissionsMixin';
import history from 'util/History';
import EventNotificationFormContainer from 'components/event-notifications/event-notification-form/EventNotificationFormContainer';
import EventNotificationActionLinks from 'components/event-notifications/event-notification-details/EventNotificationActionLinks';
import withParams from 'routing/withParams';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import { EventNotificationsActions } from 'stores/event-notifications/EventNotificationsStore';
import EventsSubareaNavigation from 'components/events/EventsSubareaNavigation';

const { isPermitted } = PermissionsMixin;

class EditEventDefinitionPage extends React.Component {
  static propTypes = {
    params: PropTypes.object.isRequired,
    currentUser: PropTypes.object.isRequired,
  };

  constructor(props) {
    super(props);

    this.state = {
      notification: undefined,
    };
  }

  componentDidMount() {
    const { params, currentUser } = this.props;

    if (isPermitted(currentUser.permissions, `eventnotifications:edit:${params.notificationId}`)) {
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
    const { params, currentUser } = this.props;

    if (!isPermitted(currentUser.permissions, `eventnotifications:edit:${params.notificationId}`)) {
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
        <EventsSubareaNavigation />
        <PageHeader title={`Edit "${notification.title}" Notification`} subactions={<EventNotificationActionLinks notificationId={notification.id} />}>
          <span>
            Notifications alert you of any configured Event when they occur. Graylog can send Notifications directly
            to you or to other systems you use for that purpose.
          </span>

          <span>
            Graylog&apos;s new Alerting system let you define more flexible and powerful rules. Learn more in the{' '}
            <DocumentationLink page={DocsHelper.PAGES.ALERTS}
                               text="documentation" />
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <EventNotificationFormContainer action="edit" notification={notification} />
          </Col>
        </Row>

      </DocumentTitle>
    );
  }
}

export default connect(withParams(EditEventDefinitionPage), {
  currentUser: CurrentUserStore,
}, ({ currentUser }) => ({ currentUser: currentUser.currentUser }));
