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
import toastr from 'toastr';

import './UserNotification.css';

const defaultSettings = {
  debug: false,
  positionClass: 'toast-bottom-full-width',
  onclick: null,
  showDuration: 300,
  hideDuration: 1000,
  timeOut: 7000,
  extendedTimeOut: 1000,
  escapeHtml: true,
  closeButton: true,
  closeHtml: '<button>Close</button>',
  progressBar: true,
  preventDuplicates: true,
};

const UserNotification = {
  error(message, title) {
    toastr.error(message, title || 'Error', {
      ...defaultSettings,
      timeOut: 10000,
    });
  },
  warning(message, title) {
    toastr.warning(message, title || 'Attention', defaultSettings);
  },
  success(message, title) {
    toastr.success(message, title || 'Information', defaultSettings);
  },
};

export default UserNotification;
