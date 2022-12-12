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
import DOMPurify from 'dompurify';

import { Alert, Button } from 'components/bootstrap';
import { RelativeTime, Icon, Spinner } from 'components/common';
import NotificationsFactory from 'logic/notifications/NotificationsFactory';
import { NotificationsActions, NotificationsStore } from 'stores/notifications/NotificationsStore';
import connect from 'stores/connect';

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

const _sanitizeDescription = (description) => {
  return DOMPurify.sanitize(description);
};

class Notification extends React.Component {
  static propTypes = {
    notification: PropTypes.object.isRequired,
    message: PropTypes.object.isRequired,
  };

  componentDidMount() {
    const { message, notification } = this.props;

    if (!message && notification) {
      NotificationsActions.getHtmlMessage(this.props.notification.type, NotificationsFactory.getValuesForNotification(notification));
    }
  }

  _onClose = () => {
    const { notification } = this.props;

    // eslint-disable-next-line no-alert
    if (window.confirm('Really delete this notification?')) {
      NotificationsActions.delete(notification.type);
    }
  };

  /* eslint-disable react/no-danger */
  render() {
    const { notification, message } = this.props;

    if (!message) {
      return <Spinner />;
    }

    return (
      <StyledAlert bsStyle="danger">
        <StyledButton className="delete-notification" bsStyle="link" onClick={this._onClose}>
          <Icon name="times" />
        </StyledButton>

        <NotificationHead>
          <Icon name="bolt" />{' '}
          <span />
          {message.title}{' '}

          <NotificationTimestamp>
            (triggered <RelativeTime dateTime={notification.timestamp} />)
          </NotificationTimestamp>
        </NotificationHead>
        <div dangerouslySetInnerHTML={{ __html: _sanitizeDescription(message.description) }}
             className="notification-description" />
      </StyledAlert>
    );
  }
}

export default connect(Notification, { notificationsStore: NotificationsStore }, ({ notificationsStore }) => {
  return { message: notificationsStore.message };
});
