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
import React from 'react';
import posthog from 'posthog-js';
import { PostHogProvider } from 'posthog-js/react';

import { useStore } from 'stores/connect';
import { TelemetrySettingsStore } from 'stores/telemetry/TelemetrySettingsStore';
import AppConfig from 'util/AppConfig';

type PostHogSettings = {
  host: string;
  key: string;
  debug: boolean;
}
const POSTHOG_DEBUG = true;

const getPostHogSettings = (): PostHogSettings => {
  const { host, api_key: key } = AppConfig.telemetry();

  return {
    host: host,
    key: key,
    debug: POSTHOG_DEBUG,
  };
};

const init = () => {
  const { host, key, debug } = getPostHogSettings();

  posthog.init(
    key,
    {
      autocapture: false,
      api_host: host,
      capture_pageview: false,
      capture_pageleave: false,
    },
  );

  if (debug) {
    posthog.debug();
  } else {
    // There is no way to disable debug mode in posthog.js once it has been enabled,
    // so we need to do it manually by removing the localStorage key.
    try {
      window.localStorage.removeItem('ph_debug');
    } catch (e) {
      // ignore
    }
  }

  return posthog;
};

const TelemetryInit = ({ children }: { children: React.ReactElement }) => {
  const settings = useStore(TelemetrySettingsStore, (state) => state.telemetrySettings);
  const { host, key } = getPostHogSettings();

  if ((settings && !settings.telemetry_enabled) || !host || !key) {
    return children;
  }

  return (
    <PostHogProvider client={init()}>
      {children}
    </PostHogProvider>
  );
};

export default TelemetryInit;
