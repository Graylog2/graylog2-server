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
import { useQuery } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import { MenuItem, ButtonGroup, DropdownButton, Button } from 'components/bootstrap';
import { Icon, Pluralize, Spinner, HoverForHelp } from 'components/common';
import { RefreshActions } from 'views/stores/RefreshStore';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId, qualifyUrl } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import UserNotification from 'preflight/util/UserNotification';
import useAutoRefresh from 'views/hooks/useAutoRefresh';

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

  const intervalDuration = moment.duration(refreshConfig.interval);
  const naturalInterval = intervalDuration.asSeconds() < 60
    ? (
      <span>{intervalDuration.asSeconds()} <Pluralize singular="second"
                                                      plural="seconds"
                                                      value={intervalDuration.asSeconds()} />
      </span>
    )
    : (
      <span>{intervalDuration.asMinutes()} <Pluralize singular="minute"
                                                      plural="minutes"
                                                      value={intervalDuration.asMinutes()} />
      </span>
    );

  return <>Every {naturalInterval}</>;
};

const useDisableOnFormChange = () => {
  const { refreshConfig } = useAutoRefresh();
  const { dirty, isSubmitting } = useFormikContext();

  useEffect(() => {
    if (refreshConfig?.enabled && !isSubmitting && dirty) {
      RefreshActions.disable();
    }
  }, [dirty, isSubmitting, refreshConfig?.enabled]);
};

const durationToMS = (duration: string) => moment.duration(duration).asMilliseconds();

const useMinimumRefreshInterval = () => {
  const { data, isInitialLoading } = useQuery(
    ['system', 'configuration', 'minimum-refresh-interval'],
    () => fetch('GET', qualifyUrl('/system/configuration/minimum_auto_refresh_interval')),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading system configuration "minimum_auto_refresh_interval" failed with status: ${errorThrown}`,
          'Could not load streams');
      },
      keepPreviousData: true,
    },
  );

  return { data, isInitialLoading };
};

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
        <Icon name={refreshConfig?.enabled ? 'pause' : 'play'} />
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
                  Interval &ldquo;{interval}&rdquo; is below configured minimum interval
                  &ldquo;{minimumRefreshInterval}&rdquo;
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
