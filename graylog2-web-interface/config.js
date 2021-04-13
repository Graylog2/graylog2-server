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
window.appConfig = {
  gl2ServerUrl: '/api',
  gl2AppPathPrefix: '',
  rootTimeZone: 'UTC',
  pluginUISettings: {
    'org.graylog.plugins.customization.theme': {},
    'org.graylog.plugins.customization.notifications': {
      '607468afaaa2380afe0757f1': {
        title: "A really long title that really shouldn't be this long but people sometimes are do it",
        shortMessage: 'zxcvzxcv',
        longMessage: 'zxcvzxcvzxcvzxcvzxcvzxcv',
        atLogin: true,
        variant: 'warning',
        hiddenTitle: true,
        isActive: true,
        isGlobal: false,
        isDismissible: false,
      },
      '6075a2999f4efa083977b75b': {
        title: 'xcvbxcvb',
        shortMessage: 'xcvbxcvb',
        longMessage: 'xcvbxcvbxcvbxcvbxcvbxcvb',
        atLogin: true,
        variant: 'danger',
        hiddenTitle: false,
        isActive: true,
        isGlobal: true,
        isDismissible: true,
      },
    },
  },
  isCloud: false,
};
