import React from 'react';
import posthog from 'posthog-js';
import { PostHogProvider } from 'posthog-js/react';

import navigationTelemetry from 'telemetry/navigationTelemetry';
import AppConfig from 'util/AppConfig';

type PostHogSettings = {
  host: string;
  key: string;
  debug: boolean;
  isDisabled: boolean,
}
const POSTHOG_DEBUG = true;

const getPostHogSettings = (): PostHogSettings => {
  const { api_key: key, host, enabled: telemetryEnabled } = AppConfig.telemetry();

  const isDisabled = telemetryEnabled || !key || !host;

  return {
    host: host,
    key: key,
    debug: POSTHOG_DEBUG,
    isDisabled: isDisabled,
  };
};

const onPostHogLoaded = (postHog) => {
  navigationTelemetry(postHog);
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
      loaded: onPostHogLoaded,
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
