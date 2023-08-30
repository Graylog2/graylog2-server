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

import { MenuItem, ButtonGroup, DropdownButton, Button } from 'components/bootstrap';
import { Icon, Pluralize } from 'components/common';
import { RefreshActions } from 'views/stores/RefreshStore';
import useRefreshConfig from 'views/components/searchbar/useRefreshConfig';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

const FlexibleButtonGroup = styled(ButtonGroup)`
  display: flex;
  justify-content: flex-end;

  > .btn-group {
    .btn:first-child {
      max-width: 100%;
    }
  }
`;

const ButtonLabel = ({ refreshConfigEnabled, naturalInterval }: {
  refreshConfigEnabled: boolean,
  naturalInterval: React.ReactNode
}) => {
  const buttonText = refreshConfigEnabled ? <>Every {naturalInterval}</> : 'Not updating';

  // eslint-disable-next-line react/jsx-no-useless-fragment
  return <>{buttonText}</>;
};

const durationToMS = (duration: string) => moment.duration(duration).asMilliseconds();

const RefreshControls = () => {
  const refreshConfig = useRefreshConfig();
  const sendTelemetry = useSendTelemetry();
  const { config: { auto_refresh_timerange_options: autoRefreshTimerangeOptions = {} } } = useSearchConfiguration();

  const _onChange = (interval: number) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_REFRESH_CONTROL_PRESET_SELECTED, {
      app_pathname: 'search',
      app_section: 'search-bar',
      app_action_value: 'refresh-search-control-dropdown',
      event_details: { interval: interval },
    });

    RefreshActions.setInterval(interval);
  };

  useEffect(() => () => RefreshActions.disable(), []);

  const _toggleEnable = useCallback(() => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_REFRESH_CONTROL_TOGGLED, {
      app_pathname: 'search',
      app_section: 'search-bar',
      app_action_value: 'refresh-search-control-enable',
      event_details: { enabled: !refreshConfig.enabled },
    });

    if (refreshConfig.enabled) {
      RefreshActions.disable();
    } else {
      RefreshActions.enable();
    }
  }, [refreshConfig?.enabled, sendTelemetry]);

  const intervalOptions = Object.entries(autoRefreshTimerangeOptions).map(([interval, label]) => (
    <MenuItem key={`RefreshControls-${label}`} onClick={() => _onChange(durationToMS(interval))}>{label}</MenuItem>
  ));
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

  return (
    <FlexibleButtonGroup aria-label="Refresh Search Controls">
      <Button onClick={_toggleEnable} title={refreshConfig.enabled ? 'Pause Refresh' : 'Start Refresh'}>
        {refreshConfig.enabled ? <Icon name="pause" /> : <Icon name="play" />}
      </Button>

      <DropdownButton title={<ButtonLabel refreshConfigEnabled={refreshConfig.enabled} naturalInterval={naturalInterval} />}
                      id="refresh-options-dropdown">
        {intervalOptions}
      </DropdownButton>
    </FlexibleButtonGroup>
  );
};

export default RefreshControls;
