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
import React, { useCallback, useMemo, useState } from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import styled, { css } from 'styled-components';

import { Icon, IfPermitted } from 'components/common';
import { DropdownButton, MenuItem } from 'components/bootstrap';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useUserDateTime from 'hooks/useUserDateTime';
import { onInitializingTimerange } from 'views/components/TimerangeForForm';
import type { TimeRange } from 'views/logic/queries/Query';
import ToolsStore from 'stores/tools/ToolsStore';
import type { SearchesConfig } from 'components/search/SearchConfig';
import { isTypeRelativeWithEnd } from 'views/typeGuards/timeRange';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import type { BsSize } from 'components/bootstrap/types';

type PresetOption = {
  eventKey?: TimeRange,
  key?: string,
  disabled: boolean,
  label: string,
}

const ExternalIcon = styled(Icon)`
  margin-left: 6px;
`;

const AdminMenuItem: React.ComponentType<React.ComponentProps<typeof MenuItem>> = styled(MenuItem)(({ theme }) => css`
  font-size: ${theme.fonts.size.small};
`);

const relativeStartTimeForTimeRange = (timeRange: TimeRange) => {
  switch (timeRange.type) {
    case 'relative':
      if (isTypeRelativeWithEnd(timeRange)) {
        return timeRange.from;
      }

      return timeRange.range;
    case 'absolute':
      return moment().diff(timeRange.from, 'seconds');
    case 'keyword':
      return ToolsStore.testNaturalDate(timeRange.keyword, timeRange.timezone).then(
        ({ from }) => moment().diff(from, 'seconds'),
      );
    default:
      throw Error('Time range type doesn\'t not exist');
  }
};

const filterOptionsByLimit = async (presets: SearchesConfig['quick_access_timerange_presets'], timeRangeLimit: number) => {
  const filteredOptions = await Promise.all(presets?.map(
    async (preset) => {
      const relativeStartTime = await relativeStartTimeForTimeRange(preset.timerange);

      return ((relativeStartTime && relativeStartTime <= timeRangeLimit) ? preset : null);
    },
  ));

  return filteredOptions.filter((opt) => !!opt);
};

const preparePresetOptions = async (presets: SearchesConfig['quick_access_timerange_presets'], timeRangeLimit: number, disabled: boolean) => {
  const availableOptions = timeRangeLimit ? await filterOptionsByLimit(presets, timeRangeLimit) : presets;

  if (availableOptions?.length) {
    return availableOptions.map(({ description, timerange, id }) => ({
      eventKey: timerange,
      key: `timerange-option-${id}`,
      disabled,
      label: description,
    }));
  }

  return [{
    disabled: true,
    label: 'No available presets',
    key: 'no-available-presets',
  }];
};

const usePresetOptions = (disabled: boolean) => {
  const { config } = useSearchConfiguration();
  const [presetOptions, setPresetOptions] = useState<Array<PresetOption> | undefined>();
  const timeRangeLimit = useMemo(() => moment.duration(config?.query_time_range_limit).asSeconds(), [config?.query_time_range_limit]);

  const onSetOptions = useCallback(async () => {
    setPresetOptions(
      await preparePresetOptions(
        config?.quick_access_timerange_presets,
        timeRangeLimit,
        disabled,
      ),
    );
  }, [config?.quick_access_timerange_presets, disabled, timeRangeLimit]);

  return ({ options: presetOptions, setOptions: onSetOptions });
};

type Props = {
  onToggle?: (open: boolean) => void,
  className?: string,
  displayTitle?: boolean,
  bsSize?: BsSize,
  header: string,
  disabled?: boolean,
  onChange?: (timerange: TimeRange) => void,
};

const TimeRangePresetDropdown = ({
  disabled,
  onChange,
  onToggle: onToggleProp,
  className,
  displayTitle,
  bsSize,
  header,
}: Props) => {
  const sendTelemetry = useSendTelemetry();
  const { formatTime } = useUserDateTime();
  const location = useLocation();
  const { options, setOptions: setDropdownOptions } = usePresetOptions(disabled);

  const _onChange = useCallback((timerange: any) => {
    if (timerange !== null && timerange !== undefined) {
      onChange(onInitializingTimerange(timerange, formatTime));
    }

    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_TIMERANGE_PRESET_SELECTED, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_section: 'search-bar',
      app_action_value: 'timerange-preset-selector',
      event_details: { timerange },
    });
  }, [formatTime, location.pathname, onChange, sendTelemetry]);

  const onToggle = useCallback(async (isOpen: boolean) => {
    if (typeof onToggleProp === 'function') {
      onToggleProp(isOpen);
    }
  }, [onToggleProp]);

  const onMouseDown = useCallback(async () => {
    await setDropdownOptions();
  }, [setDropdownOptions]);

  return (
    <DropdownButton title={displayTitle && 'Load Preset'}
                    id="relative-timerange-selector"
                    aria-label="Open time range preset select"
                    bsSize={bsSize}
                    className={className}
                    onToggle={onToggle}
                    onMouseDown={onMouseDown}>
      {header && (
        <MenuItem header>{header}</MenuItem>
      )}
      {options ? options.map(({ eventKey, key, disabled: isDisabled, label }) => (
        <MenuItem key={key} disabled={isDisabled} onClick={() => _onChange(eventKey)}>
          {label}
        </MenuItem>
      )) : (
        <MenuItem eventKey="loading" key="loading" disabled>
          Loading...
        </MenuItem>
      )}
      <IfPermitted permissions="clusterconfigentry:edit">
        <MenuItem divider />
        <AdminMenuItem href="/system/configurations" target="_blank">
          Configure presets <ExternalIcon name="external-link-alt" />
        </AdminMenuItem>
      </IfPermitted>
    </DropdownButton>
  );
};

TimeRangePresetDropdown.propTypes = {
  bsSize: PropTypes.string,
  className: PropTypes.string,
  disabled: PropTypes.bool,
  displayTitle: PropTypes.bool,
  header: PropTypes.string,
  onChange: PropTypes.func,
  onToggle: PropTypes.func,
};

TimeRangePresetDropdown.defaultProps = {
  bsSize: 'small',
  className: undefined,
  disabled: false,
  onChange: undefined,
  onToggle: undefined,
  header: undefined,
  displayTitle: true,
};

export default TimeRangePresetDropdown;
