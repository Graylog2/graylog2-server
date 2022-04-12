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
import * as Immutable from 'immutable';

declare global {
  const DEVELOPMENT: boolean | undefined;
  const FEATURES: string | undefined;
  const IS_CLOUD: boolean | undefined;
}

export type AppConfigs = {
  gl2ServerUrl: string,
  gl2AppPathPrefix: string,
  rootTimeZone: string,
  isCloud: boolean,
  pluginUISettings: { [key: string]: {} },
  featureFlags: { [key: string]: string },
};

declare global {
  interface Window {
    appConfig: AppConfigs
  }
}

const appConfig = (): AppConfigs => {
  return (window.appConfig || {}) as AppConfigs;
};

const getEnabledFeatures = () => {
  return Immutable.Map(appConfig().featureFlags)
    .filter((value) => value.trim().toLowerCase() === 'on')
    .keySeq().toList()
    .filter((s) => typeof s === 'string');
};

const AppConfig = {
  features: getEnabledFeatures(),
  gl2ServerUrl() {
    return appConfig().gl2ServerUrl;
  },

  gl2AppPathPrefix() {
    return appConfig().gl2AppPathPrefix;
  },

  gl2DevMode() {
    // The DEVELOPMENT variable will be set by webpack via the DefinePlugin.
    // eslint-disable-next-line no-undef
    return typeof (DEVELOPMENT) !== 'undefined' && DEVELOPMENT;
  },

  isFeatureEnabled(feature: string) {
    return this.features && this.features
      .map((s) => s.trim().toLowerCase())
      .includes(feature.toLowerCase());
  },

  rootTimeZone() {
    return appConfig().rootTimeZone;
  },

  isCloud() {
    if (typeof IS_CLOUD !== 'undefined') {
      // The IS_CLOUD variable will be set by webpack via the DefinePlugin.
      // eslint-disable-next-line no-undef
      return IS_CLOUD;
    }

    return appConfig().isCloud;
  },

  customThemeColors() {
    return appConfig()?.pluginUISettings?.['org.graylog.plugins.customization.theme'] ?? {};
  },

  publicNotifications() {
    return appConfig()?.pluginUISettings?.['org.graylog.plugins.customization.notifications'] ?? {};
  },

};

export default AppConfig;
