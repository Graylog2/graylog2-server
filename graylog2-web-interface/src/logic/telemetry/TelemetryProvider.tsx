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
import React, { useMemo, useEffect } from 'react';
import { usePostHog } from 'posthog-js/react';
import { useQuery } from '@tanstack/react-query';

import { Telemetry } from '@graylog/server-api';
import type { TelemetryEventType, TelemetryEvent } from 'logic/telemetry/TelemetryContext';
import TelemetryContext from 'logic/telemetry/TelemetryContext';

const TELEMETRY_CLUSTER_INFO_QUERY_KEY = 'telemetry.cluster.info';

const useTelemetryClusterInfo = () => {
  return useQuery([TELEMETRY_CLUSTER_INFO_QUERY_KEY], () => Telemetry.get(), {
    retry: 0,
    keepPreviousData: true,
    notifyOnChangeProps: ['data', 'error'],
  });
};

const TelemetryProvider = ({ children }: { children: React.ReactElement }) => {
  const posthog = usePostHog();
  const { data: telemetryClusterInfo, isSuccess: telemetryClusterInfoLoaded } = useTelemetryClusterInfo();

  useEffect(() => {
    const setGroup = () => {
      if (telemetryClusterInfoLoaded && telemetryClusterInfo?.cluster_info.cluster_id) {
        const {
          current_user: currentUser,
          cluster_info: clusterInfo,
          cluster_info: {
            cluster_id: clusterId,
          },
          license_info: licenseInfo,
        } = telemetryClusterInfo;
        posthog.group('company', clusterId, { ...clusterInfo, licenseInfo });
        posthog.identify(currentUser.user, currentUser);
      }
    };

    if (posthog) {
      setGroup();
    }
  }, [posthog, telemetryClusterInfo, telemetryClusterInfo?.cluster_info.cluster_id, telemetryClusterInfoLoaded]);

  const TelemetryContextValue = useMemo(() => {
    const sendTelemetry = (eventType: TelemetryEventType, event: TelemetryEvent) => {
      if (posthog) {
        posthog.capture(eventType, event);
      }
    };

    return ({
      sendTelemetry,
    });
  }, [posthog]);

  return (
    <TelemetryContext.Provider value={TelemetryContextValue}>
      {children}
    </TelemetryContext.Provider>
  );
};

export default TelemetryProvider;
