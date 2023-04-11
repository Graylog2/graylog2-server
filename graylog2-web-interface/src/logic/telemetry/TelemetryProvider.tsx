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
import React, { useMemo, useEffect, useState } from 'react';
import { usePostHog } from 'posthog-js/react';
import { useQuery } from '@tanstack/react-query';

import { Telemetry } from '@graylog/server-api';
import type { TelemetryEventType, TelemetryEvent } from 'logic/telemetry/TelemetryContext';
import TelemetryContext from 'logic/telemetry/TelemetryContext';
import { TelemetrySettingsActions } from 'stores/telemetry/TelemetrySettingsStore';
import TelemetryInfoModal from 'logic/telemetry/TelemetryInfoModal';

const TELEMETRY_CLUSTER_INFO_QUERY_KEY = 'telemetry.cluster.info';
type TelemetryDataType = {
  current_user: {
    [key: string]: string,
  },
  user_telemetry_settings: {
    [key: string]: string,
  },
  cluster: {
    [key: string]: string,
  },
  license: {
    [key: string]: string,
  },
  plugin: {
    [key: string]: string,
  },
  search_cluster: {
    [key: string]: string,
  },
}

const useTelemetryData = () => {
  return useQuery([TELEMETRY_CLUSTER_INFO_QUERY_KEY], () => Telemetry.get() as Promise<TelemetryDataType>, {
    retry: 0,
    keepPreviousData: true,
    notifyOnChangeProps: ['data', 'error'],
  });
};

const TelemetryProvider = ({ children }: { children: React.ReactElement }) => {
  const posthog = usePostHog();
  const { data: telemetryData, isSuccess: isTelemetryDataLoaded, refetch: refetchTelemetryData } = useTelemetryData();
  const [showTelemetryInfo, setShowTelemetryInfo] = useState<boolean>(false);

  useEffect(() => {
    const setGroup = () => {
      if (isTelemetryDataLoaded
        && telemetryData
        && telemetryData.user_telemetry_settings?.telemetry_enabled) {
        const {
          cluster: { cluster_id: clusterId, ...clusterDetails },
          current_user: { user, ...userDetails },
          license,
          plugin,
          search_cluster: searchCluster,
          user_telemetry_settings: { telemetry_permission_asked: isPermissionAsked },
        } = telemetryData as TelemetryDataType;

        posthog.group('cluster', clusterId, {
          ...clusterDetails,
          ...license,
          ...plugin,
          ...searchCluster,
        });

        posthog.identify(user, { ...userDetails });
        setShowTelemetryInfo(!isPermissionAsked);
      }
    };

    if (posthog) {
      setGroup();
    }
  }, [posthog, isTelemetryDataLoaded, telemetryData]);

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

  const handleConfirmTelemetryDialog = () => {
    TelemetrySettingsActions.update({ telemetry_permission_asked: true, telemetry_enabled: true }).then(() => {
      refetchTelemetryData();
    });

    setShowTelemetryInfo(false);
  };

  return (
    <TelemetryContext.Provider value={TelemetryContextValue}>
      {children}
      <TelemetryInfoModal show={showTelemetryInfo} onConfirm={() => handleConfirmTelemetryDialog()} />
    </TelemetryContext.Provider>
  );
};

export default TelemetryProvider;
