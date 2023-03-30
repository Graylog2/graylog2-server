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
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import DOMPurify from 'dompurify';

import { Alert, Button } from 'components/bootstrap';
import { RelativeTime, Icon, Spinner } from 'components/common';
import type { NotificationType } from 'stores/notifications/NotificationsStore';
import { NotificationsActions } from 'stores/notifications/NotificationsStore';
import useNotificationMessage from 'hooks/useNotificationMessage';

type Props = {
  notification: NotificationType,
};

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

const Notification = ({ notification }: Props) => {
  const message = useNotificationMessage(notification);

  const _onClose = () => {
    // eslint-disable-next-line no-alert
    if (window.confirm('Really delete this notification?')) {
      NotificationsActions.delete(notification.type, notification.key);
    }
  };

  if (!message) {
    return <Spinner />;
  }

  /* eslint-disable react/no-danger */
  return (
    <StyledAlert bsStyle="danger">
      <StyledButton className="delete-notification" bsStyle="link" onClick={_onClose}>
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
      <div dangerouslySetInnerHTML={{ __html: _sanitizeDescription(message?.description) }}
           className="notification-description" />
    </StyledAlert>
  );
};

Notification.propTypes = {
  notification: PropTypes.exact({
    severity: PropTypes.string.isRequired,
    details: PropTypes.object,
    type: PropTypes.string.isRequired,
    key: PropTypes.string,
    timestamp: PropTypes.string.isRequired,
    node_id: PropTypes.string.isRequired,
  }).isRequired,
};

export default Notification;
