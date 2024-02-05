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
import { useContext, useRef, useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import type { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';
import { SEARCH_BAR_GAP } from 'views/components/searchbar/SearchBarLayout';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import TimeRangeFilterSettingsContext from 'views/components/contexts/TimeRangeInputSettingsContext';
import type {
  SupportedTimeRangeType,
} from 'views/components/searchbar/time-range-filter/time-range-picker/TimeRangePicker';
import TimeRangePicker from 'views/components/searchbar/time-range-filter/time-range-picker/index';
import { NO_TIMERANGE_OVERRIDE } from 'views/Constants';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';

import TimeRangeFilterButtons from './TimeRangeFilterButtons';
import TimeRangeDisplay from './TimeRangeDisplay';

const FlexContainer = styled.div`
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  flex: 1;
  min-width: 430px;
  gap: ${SEARCH_BAR_GAP};
  position: relative;
`;

type Props = {
  className?: string,
  disabled?: boolean,
  hasErrorOnMount?: boolean,
  limitDuration: number,
  noOverride?: boolean,
  onChange: (timeRange: TimeRange | NoTimeRangeOverride) => void,
  position?: 'bottom' | 'right',
  showPresetDropdown?: boolean,
  validTypes?: Array<SupportedTimeRangeType>,
  value: TimeRange | NoTimeRangeOverride,
  withinPortal?: boolean,
};

const TimeRangeFilter = ({
  disabled,
  hasErrorOnMount,
  noOverride,
  value = NO_TIMERANGE_OVERRIDE,
  onChange,
  validTypes,
  position,
  className,
  showPresetDropdown = true,
  limitDuration,
  withinPortal,
}: Props) => {
  const containerRef = useRef();
  const { showDropdownButton } = useContext(TimeRangeFilterSettingsContext);
  const sendTelemetry = useSendTelemetry();
  const location = useLocation();
  const [show, setShow] = useState(false);

  if (validTypes && value && 'type' in value && !validTypes.includes(value?.type)) {
    throw new Error(`Value is of type ${value.type}, but only these types are valid: ${validTypes}`);
  }

  const toggleShow = () => {
    setShow(!show);

    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_TIMERANGE_PICKER_TOGGLED, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_section: 'search-bar',
      app_action_value: 'time-range-picker',
      event_details: {
        showing: !show,
      },
    });
  };

  const hideTimeRangeDropDown = () => show && toggleShow();

  return (
    <TimeRangePicker show={show}
                     currentTimeRange={value}
                     limitDuration={limitDuration}
                     noOverride={noOverride}
                     setCurrentTimeRange={onChange}
                     toggleDropdownShow={toggleShow}
                     validTypes={validTypes}
                     position={position}
                     withinPortal={withinPortal}>
      <FlexContainer className={className} ref={containerRef}>
        {showDropdownButton && (
        <TimeRangeFilterButtons disabled={disabled}
                                toggleShow={toggleShow}
                                onPresetSelectOpen={hideTimeRangeDropDown}
                                setCurrentTimeRange={onChange}
                                showPresetDropdown={showPresetDropdown}
                                hasErrorOnMount={hasErrorOnMount} />
        )}
        <TimeRangeDisplay timerange={value} toggleDropdownShow={toggleShow} />
      </FlexContainer>
    </TimeRangePicker>
  );
};

TimeRangeFilter.propTypes = {
  className: PropTypes.string,
  disabled: PropTypes.bool,
  hasErrorOnMount: PropTypes.bool,
  noOverride: PropTypes.bool,
  validTypes: PropTypes.arrayOf(PropTypes.string),
  withinPortal: PropTypes.bool,
};

TimeRangeFilter.defaultProps = {
  className: undefined,
  disabled: false,
  hasErrorOnMount: false,
  noOverride: false,
  validTypes: undefined,
  position: 'bottom-start',
  showPresetDropdown: true,
  withinPortal: true,
};

export default TimeRangeFilter;
