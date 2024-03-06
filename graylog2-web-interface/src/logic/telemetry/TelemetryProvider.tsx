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
import React, { useMemo, useEffect, useState, useCallback } from 'react';
import { usePostHog } from 'posthog-js/react';
import { useTheme } from 'styled-components';

import { getPathnameWithoutId } from 'util/URLUtils';
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
      traffic_last_month,
      users_count,
      license_count,
      node_leader_app_version,
      installation_source,
    },
    license,
    current_user,
  } = telemetryData;

  return {
    cluster_id,
    cluster_creation_date,
    installation_source,
    nodes_count,
    traffic_last_month,
    users_count,
    license_count,
    node_leader_app_version,
    ...license,
    ...current_user,
  };
};

const TelemetryProvider = ({ children }: { children: React.ReactElement }) => {
  const posthog = usePostHog();
  const theme = useTheme();

  const isPosthogLoaded = useCallback(() => (posthog?.__loaded === true), [posthog]);

  const { data: telemetryData, isSuccess: isTelemetryDataLoaded, refetch: refetchTelemetryData } = useTelemetryData();
  const [showTelemetryInfo, setShowTelemetryInfo] = useState<boolean>(false);
  const [globalProps, setGlobalProps] = useState(undefined);

  useEffect(() => {
    const app_pathname = getPathnameWithoutId(window.location.pathname);

    const setGroup = () => {
      if (isTelemetryDataLoaded
        && telemetryData
        && telemetryData.user_telemetry_settings?.telemetry_enabled) {
        const {
          cluster: { cluster_id: clusterId, ...clusterDetails },
          current_user: { user },
          license,
          plugin,
          search_cluster: searchCluster,
          data_nodes: dataNodes,
          user_telemetry_settings: { telemetry_permission_asked: isPermissionAsked },
        } = telemetryData as TelemetryDataType;
        setGlobalProps(getGlobalProps(telemetryData));

        posthog.group('cluster', clusterId, {
          app_pathname,
          app_theme: theme.mode,
          ...clusterDetails,
          ...license,
          ...plugin,
          ...searchCluster,
          ...dataNodes,
          ...getGlobalProps(telemetryData),
        });

        posthog.identify(user, { ...getGlobalProps(telemetryData) });
        setShowTelemetryInfo(!isPermissionAsked);
      }
    };

    if (isPosthogLoaded()) {
      setGroup();
    }
  }, [posthog, isTelemetryDataLoaded, telemetryData, theme.mode, isPosthogLoaded]);

  const TelemetryContextValue = useMemo(() => {
    const sendTelemetry = (eventType: TelemetryEventType, event: TelemetryEvent) => {
      if (isPosthogLoaded() && globalProps) {
        try {
          posthog.capture(eventType, {
            ...event,
            ...globalProps,
            app_theme: theme.mode,
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
  }, [globalProps, isPosthogLoaded, posthog, theme.mode]);

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
