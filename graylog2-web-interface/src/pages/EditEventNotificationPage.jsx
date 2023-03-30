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
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import connect from 'stores/connect';
import { isPermitted } from 'util/PermissionsMixin';
import EventNotificationFormContainer from 'components/event-notifications/event-notification-form/EventNotificationFormContainer';
import EventNotificationActionLinks from 'components/event-notifications/event-notification-details/EventNotificationActionLinks';
import withParams from 'routing/withParams';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import { EventNotificationsActions } from 'stores/event-notifications/EventNotificationsStore';
import EventsPageNavigation from 'components/events/EventsPageNavigation';
import withHistory from 'routing/withHistory';

class EditEventDefinitionPage extends React.Component {
  static propTypes = {
    params: PropTypes.object.isRequired,
    currentUser: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired,
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
              const { history } = this.props;
              history.push(Routes.ALERTS.NOTIFICATIONS.LIST);
            }
          },
        );
    }
  }

  render() {
    const { notification } = this.state;
    const { params, currentUser, history } = this.props;

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
        <EventsPageNavigation />
        <PageHeader title={`Edit "${notification.title}" Notification`}
                    actions={<EventNotificationActionLinks notificationId={notification.id} />}
                    documentationLink={{
                      title: 'Alerts documentation',
                      path: DocsHelper.PAGES.ALERTS,
                    }}>
          <span>
            Notifications alert you of any configured Event when they occur. Graylog can send Notifications directly
            to you or to other systems you use for that purpose.
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

export default connect(withHistory(withParams(EditEventDefinitionPage)), {
  currentUser: CurrentUserStore,
}, ({ currentUser }) => ({ currentUser: currentUser.currentUser }));
