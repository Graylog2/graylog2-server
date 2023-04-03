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

// import AppConfig from 'util/AppConfig';

type PostHogSettings = {
  host: string;
  key: string;
  debug: boolean;
  isDisabled: boolean,
}
const POSTHOG_DEBUG = true;

const getPostHogSettings = (): PostHogSettings => {
  // const { api_key: key, host, enabled: telemetryEnabled } = AppConfig.telemetry();
  //
  // const isDisabled = telemetryEnabled || !key || !host;

  return {
    host: 'https://eu.posthog.com',
    key: 'phc_KJcd3d9PRkSj9FtzXtFdx5n9dgxuq9kLFMRTyM8BCbZ',
    debug: POSTHOG_DEBUG,
    isDisabled: false,
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

const TelemetryInitProvider = ({ children }: { children: React.ReactElement }) => {
  const { isDisabled, host, key } = getPostHogSettings();

  if (isDisabled || !host || !key) {
    return children;
  }

  return (
    <PostHogProvider client={init()}>
      {children}
    </PostHogProvider>
  );
};

export default TelemetryInitProvider;
