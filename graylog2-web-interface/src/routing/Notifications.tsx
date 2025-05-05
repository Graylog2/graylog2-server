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
import { Notifications as MantineNotifications } from '@mantine/notifications';
import styled, { css } from 'styled-components';

const StyledNotifications = styled(MantineNotifications)(
  ({ theme }) => css`
    .mantine-Notification-root {
      background-color: ${theme.colors.global.contentBackground};
      border-radius: 4px;
      box-shadow: 0 2px 10px rgb(0 0 0 / 20%);
    }

    .mantine-Notification-title {
      font-weight: bold;
    }

    .mantine-Notification-description {
      color: ${theme.colors.gray[10]};
    }
  `,
);

const Notifications = () => <StyledNotifications />;

export default Notifications;
