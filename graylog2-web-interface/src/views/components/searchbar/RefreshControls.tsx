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

const FlexibleButtonGroup = styled(ButtonGroup)`
  display: flex;

  > .btn-group {
    .btn:first-child {
      max-width: 100%;
    }
  }
`;

const ButtonLabel = ({ refreshConfigEnabled, naturalInterval }: {refreshConfigEnabled: boolean, naturalInterval: React.ReactNode}) => {
  let buttonText: React.ReactNode = 'Not updating';

  if (refreshConfigEnabled) {
    buttonText = <>Every {naturalInterval}</>;
  }

  // eslint-disable-next-line react/jsx-no-useless-fragment
  return <>{buttonText}</>;
};

const _onChange = (interval: number) => {
  RefreshActions.setInterval(interval);
};

const INTERVAL_OPTIONS = [
  ['1 Second', 1000],
  ['2 Seconds', 2000],
  ['5 Seconds', 5000],
  ['10 Seconds', 10000],
  ['30 Seconds', 30000],
  ['1 Minute', 60000],
  ['5 Minutes', 300000],
] as const;

const RefreshControls = () => {
  const refreshConfig = useRefreshConfig();

  useEffect(() => () => RefreshActions.disable(), []);

  const _toggleEnable = useCallback(() => {
    if (refreshConfig.enabled) {
      RefreshActions.disable();
    } else {
      RefreshActions.enable();
    }
  }, [refreshConfig?.enabled]);

  const intervalOptions = INTERVAL_OPTIONS.map(([label, interval]) => {
    return <MenuItem key={`RefreshControls-${label}`} onClick={() => _onChange(interval)}>{label}</MenuItem>;
  });
  const intervalDuration = moment.duration(refreshConfig.interval);
  const naturalInterval = intervalDuration.asSeconds() < 60
    ? <span>{intervalDuration.asSeconds()} <Pluralize singular="second" plural="seconds" value={intervalDuration.asSeconds()} /></span>
    : <span>{intervalDuration.asMinutes()} <Pluralize singular="minute" plural="minutes" value={intervalDuration.asMinutes()} /></span>;

  return (
    <FlexibleButtonGroup aria-label="Refresh Search Controls">
      <Button onClick={_toggleEnable} title={refreshConfig.enabled ? 'Pause Refresh' : 'Start Refresh'}>
        {refreshConfig.enabled ? <Icon name="pause" /> : <Icon name="play" />}
      </Button>

      <DropdownButton title={<ButtonLabel refreshConfigEnabled={refreshConfig.enabled} naturalInterval={naturalInterval} />} id="refresh-options-dropdown">
        {intervalOptions}
      </DropdownButton>
    </FlexibleButtonGroup>
  );
};

export default RefreshControls;
