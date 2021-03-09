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
const AppConfig = {
  gl2ServerUrl() {
    if (typeof (GRAYLOG_API_URL) !== 'undefined') {
      // The GRAYLOG_API_URL variable will be set by webpack via the DefinePlugin.
      // eslint-disable-next-line no-undef
      return GRAYLOG_API_URL;
    }

    return this.appConfig().gl2ServerUrl;
  },

  gl2AppPathPrefix() {
    return this.appConfig().gl2AppPathPrefix;
  },

  gl2DevMode() {
    // The DEVELOPMENT variable will be set by webpack via the DefinePlugin.
    // eslint-disable-next-line no-undef
    return typeof (DEVELOPMENT) !== 'undefined' && DEVELOPMENT;
  },

  isFeatureEnabled(feature) {
    // eslint-disable-next-line no-undef
    return typeof (FEATURES) !== 'undefined' && FEATURES.split(',')
      .filter((s) => typeof s === 'string')
      .map((s) => s.trim().toLowerCase())
      .includes(feature.toLowerCase());
  },

  rootTimeZone() {
    return this.appConfig().rootTimeZone;
  },

  customThemeColors() {
    return this.appConfig()?.pluginUISettings?.['org.graylog.plugins.customization.theme'] ?? {};
  },

  appConfig() {
    return window.appConfig || {};
  },
};

export default AppConfig;
