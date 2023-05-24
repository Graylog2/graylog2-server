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
import { notifications } from '@mantine/notifications';

const UserNotification = {
  error(message: string, title = 'Error') {
    notifications.show({
      message,
      title,
      autoClose: 10000,
      color: 'red',
    });
  },
  warning(message: string, title = 'Attention') {
    notifications.show({
      message,
      title,
    });
  },
  success(message: string, title = 'Success') {
    notifications.show({
      message,
      title,
      color: 'green',
    });
  },
};

export default UserNotification;
