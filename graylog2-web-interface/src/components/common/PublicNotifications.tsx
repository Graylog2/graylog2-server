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
import React, { useMemo } from 'react';
import styled, { css } from 'styled-components';

import type { PublicNotificationsHooks, Notification } from 'theme/types';
import usePluginEntities from 'hooks/usePluginEntities';
import Alert from 'components/bootstrap/Alert';
import Button from 'components/bootstrap/Button';
import AppConfig from 'util/AppConfig';
import useDisclosure from 'util/hooks/useDisclosure';

interface Props {
  login?: boolean;
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

const LongContent = styled.div<{ $visible: boolean }>(
  ({ $visible }) => css`
    white-space: pre-wrap;
    display: ${$visible ? 'block' : 'none'};
    padding-top: 12px;
  `,
);

const StyledAlert = styled(Alert)`
  margin-bottom: 6px;
  padding-right: 9px;
`;

const Wrapper = styled.div`
  width: 90%;
  margin: 0 auto 15px;
`;

const AlertContainer = styled.div(
  ({ theme }) => css`
    background-color: ${theme.colors.global.contentBackground};
  `,
);

const defaultNotifications: PublicNotificationsHooks = {
  usePublicNotifications: () => ({
    notifications: undefined,
    dismissedNotifications: undefined,
    onDismissPublicNotification: undefined,
  }),
};

type PublicNotificationProps = {
  notificationId: string;
  notification: Notification;
  onDismissPublicNotification: (id: string) => void;
};
const PublicNotification = ({ notificationId, notification, onDismissPublicNotification }: PublicNotificationProps) => {
  const [showReadMore, { toggle: toggleReadMore }] = useDisclosure(false);

  const { variant, hiddenTitle, isActive, isDismissible, title, shortMessage, longMessage } = notification;

  if (!isActive) {
    return null;
  }

  const _dismiss = () => onDismissPublicNotification(notificationId);

  return (
    <AlertContainer key={title}>
      <StyledAlert bsStyle={variant} onDismiss={isDismissible ? _dismiss : undefined} title={!hiddenTitle && title}>
        <FlexWrap>
          <ShortContent>{shortMessage}</ShortContent>
          {longMessage && (
            <Button bsStyle="link" onClick={toggleReadMore}>
              Read {showReadMore ? 'Less' : 'More'}
            </Button>
          )}
        </FlexWrap>
        {longMessage && <LongContent $visible={showReadMore}>{longMessage}</LongContent>}
      </StyledAlert>
    </AlertContainer>
  );
};

const PublicNotifications = ({ login = false }: Props) => {
  const customizationHook = usePluginEntities('customization.publicNotifications');
  const { usePublicNotifications } = customizationHook[0]?.hooks || defaultNotifications;
  const { notifications, dismissedNotifications, onDismissPublicNotification } = usePublicNotifications();

  const allNotification = useMemo(
    () => (login ? AppConfig.publicNotifications() : notifications),
    [login, notifications],
  );

  const publicNotifications = useMemo(
    () =>
      Object.keys(allNotification ?? {})
        .filter((notificationId) => !dismissedNotifications?.has(notificationId))
        .filter((notificationId) => {
          const notification = allNotification[notificationId];

          return login ? notification.atLogin : notification.isGlobal;
        })
        .map((notificationId) => {
          const notification = allNotification[notificationId];

          return (
            <PublicNotification
              key={notificationId}
              notification={notification}
              notificationId={notificationId}
              onDismissPublicNotification={onDismissPublicNotification}
            />
          );
        }),
    [allNotification, dismissedNotifications, login, onDismissPublicNotification],
  );

  return publicNotifications.length ? <Wrapper>{publicNotifications}</Wrapper> : null;
};

export default PublicNotifications;
