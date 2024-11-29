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
import { useCallback, useEffect } from 'react';
import { useFormikContext } from 'formik';

import useSearchConfiguration from 'hooks/useSearchConfiguration';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import useAutoRefresh from 'views/hooks/useAutoRefresh';
import useMinimumRefreshInterval from 'views/hooks/useMinimumRefreshInterval';
import RefreshControls from 'components/common/RefreshControls';
import useDefaultInterval from 'views/hooks/useDefaultIntervalForRefresh';

const useDisableOnFormChange = () => {
  const { refreshConfig, stopAutoRefresh } = useAutoRefresh();
  const { dirty, isSubmitting } = useFormikContext();

  useEffect(() => {
    if (refreshConfig?.enabled && !isSubmitting && dirty) {
      stopAutoRefresh();
    }
  }, [dirty, isSubmitting, refreshConfig?.enabled, stopAutoRefresh]);
};

type Props = {
  disable?: boolean
}

const ViewsRefreshControls = ({ disable = false }: Props) => {
  const { dirty, submitForm } = useFormikContext();
  const location = useLocation();
  const sendTelemetry = useSendTelemetry();
  const { config } = useSearchConfiguration();
  const autoRefreshTimerangeOptions = config?.auto_refresh_timerange_options;
  const { data: minimumRefreshInterval, isInitialLoading: isLoadingMinimumInterval } = useMinimumRefreshInterval();

  const defaultInterval = useDefaultInterval();

  useDisableOnFormChange();
  const onSelectInterval = useCallback((interval: string) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_REFRESH_CONTROL_PRESET_SELECTED, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_section: 'search-bar',
      app_action_value: 'refresh-search-control-dropdown',
      event_details: { interval: interval },
    });

    if (dirty) {
      submitForm();
    }
  }, [dirty, location.pathname, sendTelemetry, submitForm]);

  const onToggle = useCallback((enabled) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_REFRESH_CONTROL_TOGGLED, {
      app_pathname: 'search',
      app_section: 'search-bar',
      app_action_value: 'refresh-search-control-enable',
      event_details: { enabled },
    });
  }, [sendTelemetry]);

  if (!config) {
    return null;
  }

  const intervalOptions = Object.entries(autoRefreshTimerangeOptions);

  return (
    <RefreshControls disable={disable}
                     intervalOptions={intervalOptions}
                     isLoadingMinimumInterval={isLoadingMinimumInterval}
                     minimumRefreshInterval={minimumRefreshInterval}
                     defaultInterval={defaultInterval}
                     humanName="Search"
                     onToggle={onToggle}
                     onEnable={submitForm}
                     onSelectInterval={onSelectInterval} />
  );
};

export default ViewsRefreshControls;
