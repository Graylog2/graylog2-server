import PropTypes from 'prop-types';
import React from 'react';
import styled, { css } from 'styled-components';

import { Alert, Button } from 'components/graylog';
import { Timestamp, Icon } from 'components/common';
import NotificationsFactory from 'logic/notifications/NotificationsFactory';
import ActionsProvider from 'injection/ActionsProvider';

const NotificationsActions = ActionsProvider.getActions('Notifications');

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
        <Button className="close delete-notification" onClick={this._onClose}>&times;</Button>

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
