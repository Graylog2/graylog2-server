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
import React, { useState } from 'react';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { PublicNotificationsHooks } from 'theme/types';

import { Alert, Button } from 'components/graylog';
import AppConfig from 'util/AppConfig';

const customizationHook = PluginStore.exports('customization.publicNotifications');

interface Props {
  readFromConfig?: boolean,
}

const FlexWrap = styled.div`
  display: flex;
  align-items: center;
`;

const ShortContent = styled.p`
  flex: 1;
  margin: 0;
  font-weight: bold;
`;

const LongContent = styled.div(({ $visible }: {$visible: boolean}) => css`
  white-space: pre-wrap;
  display: ${$visible ? 'block' : 'none'};
  padding-top: 12px;
`);

const defaultNotifications: PublicNotificationsHooks = {
  usePublicNotifications: () => ({
    notifications: undefined,
    dismissedNotifications: undefined,
    onDismissPublicNotification: undefined,
  }),
};

const PublicNotifications = ({ readFromConfig }: Props) => {
  const { usePublicNotifications } = customizationHook[0]?.hooks || defaultNotifications;
  const [showReadMore, setShowReadMore] = useState<string>(undefined);
  const { notifications, dismissedNotifications, onDismissPublicNotification } = usePublicNotifications();

  if (!notifications && !dismissedNotifications && !onDismissPublicNotification) {
    return null;
  }

  const allNotification = readFromConfig ? AppConfig.publicNotifications() : notifications;

  const publicNotifications = Object.keys(allNotification).map((notificationId) => {
    if (dismissedNotifications.has(notificationId)) {
      return null;
    }

    const toggleReadMore = () => {
      setShowReadMore(showReadMore ? undefined : notificationId);
    };

    const notification = allNotification[notificationId];
    const { variant, hiddenTitle, isDismissible, title, shortMessage, longMessage } = notification;

    const _dismiss = () => {
      return onDismissPublicNotification(notificationId);
    };

    return (
      <Alert bsStyle={variant} onDismiss={isDismissible ? _dismiss : undefined} key={title}>
        {!hiddenTitle && (<h3>{title}</h3>)}
        <FlexWrap>
          <ShortContent>{shortMessage}</ShortContent>
          {longMessage && <Button bsStyle="link" onClick={toggleReadMore}>Read {showReadMore === notificationId ? 'Less' : 'More'}</Button>}
        </FlexWrap>
        {longMessage && <LongContent $visible={showReadMore === notificationId}>{longMessage}</LongContent>}
      </Alert>
    );
  });

  return <>{publicNotifications}</>;
};

PublicNotifications.propTypes = {
  readFromConfig: PropTypes.bool,
};

PublicNotifications.defaultProps = {
  readFromConfig: false,
};

export default PublicNotifications;
