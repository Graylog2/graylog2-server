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

const textDefault = '#00f';
const textAlt = '#0f0';

window.appConfig = {
  gl2ServerUrl: '/api',
  gl2AppPathPrefix: '',
  rootTimeZone: 'Europe/Berlin',
  customTheme: {
    teint: {
      brand: {
        primary: '#f0f',
        secondary: textDefault,
        tertiary: textAlt,
      },
      global: {
        background: 'gray',
        contentBackground: 'black',
        link: 'aqua',
        textDefault,
        textAlt,
      },
      variant: {
        default: 'orange',
        danger: 'purple',
        info: 'blue',
        primary: 'green',
        success: 'teal',
        warning: 'yellow',
      },
    },
  },
};
