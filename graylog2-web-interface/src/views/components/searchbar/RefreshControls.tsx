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
import { Icon, Pluralize } from 'components/common';
import { RefreshActions } from 'views/stores/RefreshStore';
import useRefreshConfig from 'views/components/searchbar/useRefreshConfig';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';

const FlexibleButtonGroup = styled(ButtonGroup)`
  display: flex;
  justify-content: flex-end;

  > .btn-group {
    .btn:first-child {
      max-width: 100%;
    }
  }
`;

const ButtonLabel = ({ refreshConfigEnabled }: { refreshConfigEnabled: boolean }) => {
  const refreshConfig = useRefreshConfig();
  const intervalDuration = moment.duration(refreshConfig.interval);

  if (!refreshConfigEnabled) {
    return <>Not updating</>;
  }

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
  const refreshConfig = useRefreshConfig();
  const { dirty, isSubmitting } = useFormikContext();

  useEffect(() => {
    if (refreshConfig.enabled && !isSubmitting && dirty) {
      RefreshActions.disable();
    }
  }, [dirty, isSubmitting, refreshConfig.enabled]);
};

const durationToMS = (duration: string) => moment.duration(duration).asMilliseconds();

const RefreshControls = () => {
  const { dirty, submitForm } = useFormikContext();
  const refreshConfig = useRefreshConfig();
  const location = useLocation();
  const sendTelemetry = useSendTelemetry();
  const { config: { auto_refresh_timerange_options: autoRefreshTimerangeOptions = {} } } = useSearchConfiguration();

  useEffect(() => () => RefreshActions.disable(), []);
  useDisableOnFormChange();

  const selectInterval = useCallback((interval: number) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_REFRESH_CONTROL_PRESET_SELECTED, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_section: 'search-bar',
      app_action_value: 'refresh-search-control-dropdown',
      event_details: { interval: interval },
    });

    RefreshActions.setInterval(interval);

    if (dirty) {
      submitForm();
    }
  }, [dirty, location.pathname, sendTelemetry, submitForm]);

  const toggleEnable = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_REFRESH_CONTROL_TOGGLED, {
      app_pathname: 'search',
      app_section: 'search-bar',
      app_action_value: 'refresh-search-control-enable',
      event_details: { enabled: !refreshConfig.enabled },
    });

    if (refreshConfig.enabled) {
      RefreshActions.disable();
    } else {
      if (dirty) {
        submitForm();
      }

      RefreshActions.enable();
    }
  }, [dirty, refreshConfig.enabled, sendTelemetry, submitForm]);

  return (
    <FlexibleButtonGroup aria-label="Refresh Search Controls">
      <Button onClick={toggleEnable} title={refreshConfig.enabled ? 'Pause Refresh' : 'Start Refresh'}>
        <Icon name={refreshConfig.enabled ? 'pause' : 'play'} />
      </Button>

      <DropdownButton title={<ButtonLabel refreshConfigEnabled={refreshConfig.enabled} />}
                      id="refresh-options-dropdown">
        {Object.entries(autoRefreshTimerangeOptions).map(([interval, label]) => (
          <MenuItem key={`RefreshControls-${label}`} onClick={() => selectInterval(durationToMS(interval))}>{label}</MenuItem>
        ))}
      </DropdownButton>
    </FlexibleButtonGroup>
  );
};

export default RefreshControls;
