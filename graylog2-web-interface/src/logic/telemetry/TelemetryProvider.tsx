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

import type { TelemetryEventType, TelemetryEvent } from 'logic/telemetry/TelemetryContext';
import TelemetryContext from 'logic/telemetry/TelemetryContext';
import { TelemetrySettingsActions } from 'stores/telemetry/TelemetrySettingsStore';
import TelemetryInfoModal from 'logic/telemetry/TelemetryInfoModal';
import type { TelemetryDataType } from 'logic/telemetry/useTelemetryData';
import useTelemetryData from 'logic/telemetry/useTelemetryData';

const getGlobalProps = (telemetryData: TelemetryDataType) => {
  const {
    cluster: {
      cluster_id,
      cluster_creation_date,
      nodes_count,
      average_last_month_traffic,
      users_count,
      license_count,
      'node_leader.app_version': node_leader_app_version,
    },
    license,
  } = telemetryData;

  return {
    cluster_id,
    cluster_creation_date,
    nodes_count,
    average_last_month_traffic,
    users_count,
    license_count,
    node_leader_app_version,
    ...license,
  };
};

const TelemetryProvider = ({ children }: { children: React.ReactElement }) => {
  const posthog = usePostHog();
  const { data: telemetryData, isSuccess: isTelemetryDataLoaded, refetch: refetchTelemetryData } = useTelemetryData();
  const [showTelemetryInfo, setShowTelemetryInfo] = useState<boolean>(false);
  const [globalProps, setGlobalProps] = useState({});

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
        setGlobalProps(getGlobalProps(telemetryData));

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
        try {
          posthog.capture(eventType, {
            ...event,
            ...globalProps,
          });
        } catch {
          // eslint-disable-next-line no-console
          console.warn('Could not capture telemetry event.');
        }
      }
    };

    return ({
      sendTelemetry,
    });
  }, [globalProps, posthog]);

  const handleConfirmTelemetryDialog = () => {
    TelemetrySettingsActions.update({ telemetry_permission_asked: true, telemetry_enabled: true }).then(() => {
      refetchTelemetryData();
    });

    setShowTelemetryInfo(false);
  };

  return (
    <TelemetryContext.Provider value={TelemetryContextValue}>
      {children}
      {showTelemetryInfo
        && <TelemetryInfoModal show={showTelemetryInfo} onConfirm={() => handleConfirmTelemetryDialog()} />}
    </TelemetryContext.Provider>
  );
};

export default TelemetryProvider;
