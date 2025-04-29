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

export type BrandingResource = { enabled?: boolean; url?: string | undefined };
export type BrandingResourceKey = 'stream_rule_matcher_code' | 'contact_support' | 'contact_us' | 'marketplace';

export type BrandingResources = Record<BrandingResourceKey, BrandingResource>;

type Branding = {
  product_name?: string;
  favicon?: string;
  logo?: {
    light: string;
    dark: string;
  };
  login?: {
    background?: string;
  };
  welcome?: {
    news?: { enabled: boolean; feed?: string };
    releases?: { enabled: boolean };
  };
  navigation?: {
    home?: { icon: string };
    user_menu?: { icon: string };
    scratchpad?: { icon: string };
    help?: { icon: string };
  };
  help_url?: string;
  footer?: { enabled: boolean };
  resources?: BrandingResources;
};

export type AppConfigs = {
  gl2ServerUrl: string;
  gl2AppPathPrefix: string;
  rootTimeZone: string;
  isCloud: boolean;
  pluginUISettings: { [key: string]: {} };
  featureFlags: { [key: string]: string };
  telemetry: { api_key: string; host: string; enabled: boolean };
  contentStream: { refresh_interval: string; rss_url: string };
  branding: Branding | undefined;
};

declare global {
  interface Window {
    appConfig: AppConfigs;
  }
}

const appConfig = (): AppConfigs => (window.appConfig || {}) as AppConfigs;

const getEnabledFeatures = () =>
  Immutable.Map(appConfig().featureFlags)
    .filter((value) => value.trim().toLowerCase() === 'on')
    .keySeq()
    .toList()
    .filter((s) => typeof s === 'string');

const AppConfig = {
  contentStream() {
    return appConfig()?.contentStream;
  },
  features: getEnabledFeatures(),
  gl2ServerUrl() {
    return appConfig().gl2ServerUrl;
  },

  gl2AppPathPrefix() {
    return appConfig().gl2AppPathPrefix;
  },

  gl2DevMode() {
    // The DEVELOPMENT variable will be set by webpack via the DefinePlugin.

    return typeof DEVELOPMENT !== 'undefined' && DEVELOPMENT;
  },

  isFeatureEnabled(feature: string) {
    return this.features && this.features.map((s) => s.trim().toLowerCase()).includes(feature.toLowerCase());
  },

  rootTimeZone() {
    return appConfig().rootTimeZone;
  },

  isCloud() {
    if (typeof IS_CLOUD !== 'undefined') {
      // The IS_CLOUD variable will be set by webpack via the DefinePlugin.

      return IS_CLOUD;
    }

    return appConfig().isCloud;
  },

  customThemeColors() {
    return appConfig()?.pluginUISettings?.['org.graylog.plugins.customization.theme'] ?? {};
  },

  telemetry() {
    return appConfig()?.telemetry;
  },

  publicNotifications() {
    return appConfig()?.pluginUISettings?.['org.graylog.plugins.customization.notifications'] ?? {};
  },

  pluginUISettings(key: string): any {
    return appConfig()?.pluginUISettings?.[key] ?? {};
  },

  branding(): Branding | undefined {
    return appConfig()?.branding;
  },
};

export default AppConfig;
