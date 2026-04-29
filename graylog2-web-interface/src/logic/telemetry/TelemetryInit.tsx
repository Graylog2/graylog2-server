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
import * as React from 'react';
import posthog from 'posthog-js';
import { PostHogProvider } from 'posthog-js/react';

import { useStore } from 'stores/connect';
import AppConfig from 'util/AppConfig';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import useTelemetrySettings from 'logic/telemetry/useTelemetrySettings';

type PostHogSettings = {
  host: string;
  key: string;
};

const getPostHogSettings = (): PostHogSettings => {
  const { host, api_key: key } = AppConfig.telemetry() || {};

  return {
    host: host,
    key: key,
  };
};

const init = (key: string, host: string) => {
  posthog.init(key, {
    autocapture: false,
    api_host: host,
    capture_pageview: false,
    capture_pageleave: false,
    cross_subdomain_cookie: false,
    persistence: 'cookie',
  });

  return posthog;
};

const TelemetryInit = ({ children }: { children: React.ReactElement }) => {
  const { host, key } = getPostHogSettings();
  const { currentUser } = useStore(CurrentUserStore);
  const { data: settings } = useTelemetrySettings({ enabled: !!currentUser });

  if (!settings?.telemetry_enabled || !host || !key) {
    return children;
  }

  return <PostHogProvider client={init(key, host)}>{children}</PostHogProvider>;
};

export default TelemetryInit;
