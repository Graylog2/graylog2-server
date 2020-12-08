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
import PropTypes from 'prop-types';
import React from 'react';
import styled, { css } from 'styled-components';

import { Alert, Button } from 'components/graylog';
import { Timestamp, Icon } from 'components/common';
import NotificationsFactory from 'logic/notifications/NotificationsFactory';
import ActionsProvider from 'injection/ActionsProvider';

const NotificationsActions = ActionsProvider.getActions('Notifications');

const StyledButton = styled(Button)`
  float: right;
`;

const StyledAlert = styled(Alert)(({ theme }) => css`
  margin-top: 10px;

  i {
    color: ${theme.colors.gray[10]};
  }

  form {
    margin-bottom: 0;
  }
`);

const NotificationHead = styled.h3`
  margin-bottom: 5px;
`;

const NotificationTimestamp = styled.span(({ theme }) => css`
  margin-left: 3px;
  font-size: ${theme.fonts.size.small};
`);

class Notification extends React.Component {
  static propTypes = {
    notification: PropTypes.object.isRequired,
  };

  _onClose = () => {
    const { notification } = this.props;

    // eslint-disable-next-line no-alert
    if (window.confirm('Really delete this notification?')) {
      NotificationsActions.delete(notification.type);
    }
  };

  render() {
    const { notification } = this.props;
    const notificationView = NotificationsFactory.getForNotification(notification);

    return (
      <StyledAlert bsStyle="danger">
        <StyledButton className="delete-notification" bsStyle="link" onClick={this._onClose}>
          <Icon name="times" />
        </StyledButton>

        <NotificationHead>
          <Icon name="bolt" />{' '}
          {notificationView.title}{' '}

          <NotificationTimestamp>
            (triggered <Timestamp dateTime={notification.timestamp} relative />)
          </NotificationTimestamp>
        </NotificationHead>
        <div className="notification-description">
          {notificationView.description}
        </div>
      </StyledAlert>
    );
  }
}

export default Notification;
