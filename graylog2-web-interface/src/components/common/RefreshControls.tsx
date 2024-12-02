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
import React, { useCallback } from 'react';
import { styled } from 'styled-components';

import { ProgressAnimation, Icon, Spinner, HoverForHelp } from 'components/common/index';
import { Button, DropdownButton, MenuItem, ButtonGroup } from 'components/bootstrap';
import ReadableDuration from 'components/common/ReadableDuration';
import useAutoRefresh from 'views/hooks/useAutoRefresh';
import { durationToMS } from 'util/DateTime';

const FlexibleButtonGroup = styled(ButtonGroup)`
  display: flex;
  justify-content: flex-end;
  position: relative;

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

  return (
    <>
      Every <ReadableDuration duration={refreshConfig.interval} />
    </>
  );
};

type Props = {
  disable: boolean,
  intervalOptions: Array<[string, string]>,
  isLoadingMinimumInterval: boolean,
  minimumRefreshInterval: string,
  defaultInterval: string,
  onSelectInterval?: (interval: string) => void,
  onToggle?: (enabled: boolean) => void,
  onEnable?: () => void,
  onDisable?: () => void,
  humanName: string,
}

const RefreshControls = ({ humanName, disable, onToggle = null, onEnable = null, onDisable = null, intervalOptions, onSelectInterval = null, isLoadingMinimumInterval, minimumRefreshInterval, defaultInterval }: Props) => {
  const { refreshConfig, startAutoRefresh, stopAutoRefresh, animationId } = useAutoRefresh();

  const selectInterval = useCallback((interval: string) => {
    startAutoRefresh(durationToMS(interval));

    if (typeof onSelectInterval === 'function') {
      onSelectInterval(interval);
    }
  }, [onSelectInterval, startAutoRefresh]);

  const toggleEnable = useCallback(() => {
    if (!defaultInterval && !refreshConfig?.interval) {
      return;
    }

    if (typeof onToggle === 'function') {
      onToggle(!refreshConfig?.enabled);
    }

    if (refreshConfig?.enabled) {
      stopAutoRefresh();

      if (typeof onDisable === 'function') {
        onDisable();
      }
    } else {
      startAutoRefresh(refreshConfig?.interval ?? durationToMS(defaultInterval));

      if (typeof onEnable === 'function') {
        onEnable();
      }
    }
  }, [defaultInterval, onDisable, onEnable, onToggle, refreshConfig?.enabled, refreshConfig?.interval, startAutoRefresh, stopAutoRefresh]);

  return (
    <FlexibleButtonGroup aria-label={`Refresh ${humanName} Controls`}>
      {(refreshConfig?.enabled && animationId) && (
        <ProgressAnimation key={`${refreshConfig.interval}-${animationId}`}
                           $animationDuration={refreshConfig.interval}
                           $increase={false} />
      )}

      <Button onClick={toggleEnable}
              title={refreshConfig?.enabled ? 'Pause Refresh' : 'Start Refresh'}
              disabled={disable || isLoadingMinimumInterval || !defaultInterval}>
        <Icon name={refreshConfig?.enabled ? 'pause' : 'update'} />
      </Button>

      <DropdownButton title={<ButtonLabel />}
                      disabled={disable}
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
                  Interval of <ReadableDuration duration={interval} /> ({interval}) is below configured minimum interval
                  of <ReadableDuration duration={minimumRefreshInterval} /> ({minimumRefreshInterval}).
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
