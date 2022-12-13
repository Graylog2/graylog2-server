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

import type { PublicNotificationsHooks } from 'theme/types';
import usePluginEntities from 'hooks/usePluginEntities';
import Alert from 'components/bootstrap/Alert';
import Button from 'components/bootstrap/Button';
import AppConfig from 'util/AppConfig';

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

const StyledAlert = styled(Alert)`
  margin-bottom: 6px;
  padding-right: 9px;
  
  &.alert-dismissable .close {
    right: 12px;
  }
`;

const Wrapper = styled.div`
  width: 90%;
  margin: 0 auto 15px;
`;

const defaultNotifications: PublicNotificationsHooks = {
  usePublicNotifications: () => ({
    notifications: undefined,
    dismissedNotifications: undefined,
    onDismissPublicNotification: undefined,
  }),
};

const PublicNotifications = ({ readFromConfig }: Props) => {
  const customizationHook = usePluginEntities('customization.publicNotifications');
  const { usePublicNotifications } = customizationHook[0]?.hooks || defaultNotifications;
  const [showReadMore, setShowReadMore] = useState<string>(undefined);
  const { notifications, dismissedNotifications, onDismissPublicNotification } = usePublicNotifications();

  const allNotification = readFromConfig ? AppConfig.publicNotifications() : notifications;

  if (!allNotification && !dismissedNotifications && !onDismissPublicNotification) {
    return null;
  }

  const publicNotifications = Object.keys(allNotification).map((notificationId) => {
    if (dismissedNotifications?.has(notificationId)) {
      return null;
    }

    const toggleReadMore = () => {
      setShowReadMore(showReadMore ? undefined : notificationId);
    };

    const notification = allNotification[notificationId];
    const { variant, hiddenTitle, isActive, isDismissible, title, shortMessage, longMessage } = notification;

    if (!isActive) {
      return null;
    }

    const _dismiss = () => {
      return onDismissPublicNotification(notificationId);
    };

    return (
      <StyledAlert bsStyle={variant} onDismiss={isDismissible ? _dismiss : undefined} key={title}>
        {!hiddenTitle && (<h3>{title}</h3>)}
        <FlexWrap>
          <ShortContent>{shortMessage}</ShortContent>
          {longMessage && <Button bsStyle="link" onClick={toggleReadMore}>Read {showReadMore === notificationId ? 'Less' : 'More'}</Button>}
        </FlexWrap>
        {longMessage && <LongContent $visible={showReadMore === notificationId}>{longMessage}</LongContent>}
      </StyledAlert>
    );
  }).filter((a) => a);

  if (publicNotifications.length) {
    return <Wrapper>{publicNotifications}</Wrapper>;
  }

  return null;
};

PublicNotifications.propTypes = {
  readFromConfig: PropTypes.bool,
};

PublicNotifications.defaultProps = {
  readFromConfig: false,
};

export default PublicNotifications;
