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

import React, { useState, useEffect } from 'react';
import styled, { css } from 'styled-components';

import { Alert, Button } from 'components/graylog';
import AppConfig from 'util/AppConfig';
import Store from 'logic/local-storage/Store';

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

const PublicNotifications = () => {
  const localStorageItem = 'gl-notifications';
  const notificationStore = new Set(Store.get(localStorageItem) || []);
  const [showReadMore, setShowReadMore] = useState<string>(undefined);

  // useEffect(()=>{
  //   const visibleNotifications = [];
  //
  //   Object.keys(AppConfig.loginNotifications()).forEach((notificationId)=>{
  //     if (!notificationStore.has(notificationId)) {
  //       visibleNotifications.push(notificationId);
  //     }
  //   });
  // })

  const notifications = Object.keys(AppConfig.loginNotifications()).map((notificationId) => {
    if (notificationStore.has(notificationId)) {
      return null;
    }

    const toggleReadMore = () => {
      setShowReadMore(showReadMore ? undefined : notificationId);
    };

    const onDismiss = () => {
      const dismissed = Array.from(notificationStore.add(notificationId));
      Store.set(localStorageItem, dismissed);
    };

    const notification = AppConfig.loginNotifications()[notificationId];
    const { variant, hiddenTitle, isDismissible, title, shortMessage, longMessage } = notification;

    return (
      <Alert bsStyle={variant} onDismiss={isDismissible ? onDismiss : undefined}>
        {!hiddenTitle && (<h3>{title}</h3>)}
        <FlexWrap>
          <ShortContent>{shortMessage}</ShortContent>
          {longMessage && <Button bsStyle="link" onClick={toggleReadMore}>Read {showReadMore ? 'Less' : 'More'}</Button>}
        </FlexWrap>
        {longMessage && <LongContent $visible={showReadMore === notificationId}>{longMessage}</LongContent>}
      </Alert>
    );
  });

  return <>{notifications}</>;
};

export default PublicNotifications;
