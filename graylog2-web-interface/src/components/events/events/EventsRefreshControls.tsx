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
import { useCallback } from 'react';

import useSearchConfiguration from 'hooks/useSearchConfiguration';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import useMinimumRefreshInterval from 'views/hooks/useMinimumRefreshInterval';
import RefreshControls from 'components/common/RefreshControls';
import useDefaultInterval from 'views/hooks/useDefaultIntervalForRefresh';
import AutoRefreshProvider from 'views/components/contexts/AutoRefreshProvider';
import { useTableFetchContext } from 'components/common/PaginatedEntityTable';

const EventsRefreshControls = () => {
  const { refetch } = useTableFetchContext();
  const location = useLocation();
  const sendTelemetry = useSendTelemetry();
  const { config } = useSearchConfiguration();
  const autoRefreshTimerangeOptions = config?.auto_refresh_timerange_options;
  const { data: minimumRefreshInterval, isInitialLoading: isLoadingMinimumInterval } = useMinimumRefreshInterval();

  const defaultInterval = useDefaultInterval();

  const onSelectInterval = useCallback((interval: string) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.ALERTS_REFRESH_CONTROL_PRESET_SELECTED, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_section: 'alerts-page',
      app_action_value: 'refresh-alerts-control-dropdown',
      event_details: { interval: interval },
    });
  }, [location.pathname, sendTelemetry]);

  const onToggle = useCallback((enabled) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.ALERTS_REFRESH_CONTROL_TOGGLED, {
      app_pathname: 'alerts',
      app_section: 'alerts-page',
      app_action_value: 'refresh-alerts-control-enable',
      event_details: { enabled },
    });
  }, [sendTelemetry]);

  if (!config) {
    return null;
  }

  const intervalOptions = autoRefreshTimerangeOptions ? Object.entries(autoRefreshTimerangeOptions) : [];

  return (
    <AutoRefreshProvider onRefresh={refetch}>
      <RefreshControls disable={false}
                       intervalOptions={intervalOptions}
                       isLoadingMinimumInterval={isLoadingMinimumInterval}
                       minimumRefreshInterval={minimumRefreshInterval}
                       defaultInterval={defaultInterval}
                       humanName="Evets"
                       onToggle={onToggle}
                       onSelectInterval={onSelectInterval} />
    </AutoRefreshProvider>
  );
};

export default EventsRefreshControls;
