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
import { useState } from 'react';
import PropTypes from 'prop-types';

import { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';

import TimeRangeDropdownButton from './TimeRangeDropdownButton';
import TimeRangeDropdown from './date-time-picker/TimeRangeDropdown';
import TimeRangeDisplay from './TimeRangeDisplay';

type Props = {
  currentTimeRange: TimeRange | NoTimeRangeOverride,
  disabled?: boolean,
  noOverride?: boolean,
  hasErrorOnMount?: boolean,
  setCurrentTimeRange: (nextTimeRange: TimeRange | NoTimeRangeOverride) => void,
};

const TimeRangeTypeSelector = ({ disabled, hasErrorOnMount, noOverride, currentTimeRange, setCurrentTimeRange }: Props) => {
  const [show, setShow] = useState(false);

  const toggleShow = () => setShow(!show);

  return (
    <>
      <TimeRangeDropdownButton disabled={disabled}
                               show={show}
                               toggleShow={toggleShow}
                               hasErrorOnMount={hasErrorOnMount}>
        <TimeRangeDropdown currentTimeRange={currentTimeRange}
                           noOverride={noOverride}
                           setCurrentTimeRange={setCurrentTimeRange}
                           toggleDropdownShow={toggleShow} />
      </TimeRangeDropdownButton>
      <TimeRangeDisplay timerange={currentTimeRange} toggleDropdownShow={toggleShow} />
    </>
  );
};

TimeRangeTypeSelector.propTypes = {
  disabled: PropTypes.bool,
  hasErrorOnMount: PropTypes.bool,
  noOverride: PropTypes.bool,
};

TimeRangeTypeSelector.defaultProps = {
  disabled: false,
  hasErrorOnMount: false,
  noOverride: false,
};

export default TimeRangeTypeSelector;
