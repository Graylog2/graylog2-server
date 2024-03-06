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
import moment from 'moment';
import styled from 'styled-components';
import { useFormikContext } from 'formik';

import { MenuItem, ButtonGroup, DropdownButton, Button } from 'components/bootstrap';
import { Icon, Spinner, HoverForHelp } from 'components/common';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import useAutoRefresh from 'views/hooks/useAutoRefresh';
import useMinimumRefreshInterval from 'views/hooks/useMinimumRefreshInterval';
import ReadableDuration from 'components/common/ReadableDuration';

const FlexibleButtonGroup = styled(ButtonGroup)`
  display: flex;
  justify-content: flex-end;

  > .btn-group {
    .btn:first-child {
      max-width: 100%;
    }
  }
`;

const ButtonLabel = () => {
  const { refreshConfig } = useAutoRefresh();

  if (!refreshConfig?.enabled) {
    return <>Not updating</>;
  }

  return <>Every <ReadableDuration duration={refreshConfig.interval} /></>;
};

const useDisableOnFormChange = () => {
  const { refreshConfig, stopAutoRefresh } = useAutoRefresh();
  const { dirty, isSubmitting } = useFormikContext();

  useEffect(() => {
    if (refreshConfig?.enabled && !isSubmitting && dirty) {
      stopAutoRefresh();
    }
  }, [dirty, isSubmitting, refreshConfig?.enabled, stopAutoRefresh]);
};

const durationToMS = (duration: string) => moment.duration(duration).asMilliseconds();

const useDefaultInterval = () => {
  const { config: { auto_refresh_timerange_options: autoRefreshTimerangeOptions, default_auto_refresh_option: defaultAutoRefreshInterval } } = useSearchConfiguration();
  const { data: minimumInterval } = useMinimumRefreshInterval();
  const minimumIntervalInMS = durationToMS(minimumInterval);

  if (durationToMS(defaultAutoRefreshInterval) < minimumIntervalInMS) {
    const availableIntervals = Object.entries(autoRefreshTimerangeOptions)
      .filter(([interval]) => durationToMS(interval) >= minimumIntervalInMS)
      .sort(([interval1], [interval2]) => (
        durationToMS(interval1) > durationToMS(interval2) ? 1 : -1
      ));

    return availableIntervals.length ? availableIntervals[0][0] : null;
  }

  return defaultAutoRefreshInterval;
};

const RefreshControls = () => {
  const { dirty, submitForm } = useFormikContext();
  const location = useLocation();
  const sendTelemetry = useSendTelemetry();
  const { config: { auto_refresh_timerange_options: autoRefreshTimerangeOptions } } = useSearchConfiguration();
  const { data: minimumRefreshInterval, isInitialLoading: isLoadingMinimumInterval } = useMinimumRefreshInterval();
  const intervalOptions = Object.entries(autoRefreshTimerangeOptions);
  const { refreshConfig, startAutoRefresh, stopAutoRefresh } = useAutoRefresh();
  const defaultInterval = useDefaultInterval();

  useDisableOnFormChange();

  const selectInterval = useCallback((interval: string) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_REFRESH_CONTROL_PRESET_SELECTED, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_section: 'search-bar',
      app_action_value: 'refresh-search-control-dropdown',
      event_details: { interval: interval },
    });

    startAutoRefresh(durationToMS(interval));

    if (dirty) {
      submitForm();
    }
  }, [dirty, location.pathname, sendTelemetry, startAutoRefresh, submitForm]);

  const toggleEnable = useCallback(() => {
    if (!defaultInterval && !refreshConfig?.interval) {
      return;
    }

    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_REFRESH_CONTROL_TOGGLED, {
      app_pathname: 'search',
      app_section: 'search-bar',
      app_action_value: 'refresh-search-control-enable',
      event_details: { enabled: !refreshConfig?.enabled },
    });

    if (refreshConfig?.enabled) {
      stopAutoRefresh();
    } else {
      if (dirty) {
        submitForm();
      }

      startAutoRefresh(refreshConfig?.interval ?? durationToMS(defaultInterval));
    }
  }, [defaultInterval, dirty, refreshConfig?.enabled, refreshConfig?.interval, sendTelemetry, startAutoRefresh, stopAutoRefresh, submitForm]);

  return (
    <FlexibleButtonGroup aria-label="Refresh Search Controls">
      <Button onClick={toggleEnable} title={refreshConfig?.enabled ? 'Pause Refresh' : 'Start Refresh'} disabled={isLoadingMinimumInterval || !defaultInterval}>
        <Icon name={refreshConfig?.enabled ? 'pause' : 'play_arrow'} />
      </Button>

      <DropdownButton title={<ButtonLabel />}
                      id="refresh-options-dropdown">
        {isLoadingMinimumInterval && <Spinner />}
        {!isLoadingMinimumInterval && intervalOptions.map(([interval, label]) => {
          const isBelowMinimum = durationToMS(interval) < durationToMS(minimumRefreshInterval);

          return (
            <MenuItem key={`RefreshControls-${label}`}
                      onClick={() => selectInterval(interval)}
                      disabled={isBelowMinimum}>
              {label}
              {isBelowMinimum && (
                <HoverForHelp displayLeftMargin>
                  Interval of <ReadableDuration duration={interval} /> ({interval}) is below configured minimum interval of <ReadableDuration duration={minimumRefreshInterval} /> ({minimumRefreshInterval}).
                </HoverForHelp>
              )}
            </MenuItem>
          );
        })}
      </DropdownButton>
    </FlexibleButtonGroup>
  );
};

export default RefreshControls;
