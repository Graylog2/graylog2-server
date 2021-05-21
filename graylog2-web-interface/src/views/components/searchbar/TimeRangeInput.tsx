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
import styled from 'styled-components';

import { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';

import TimeRangeDropdownButton from './TimeRangeDropdownButton';
import TimeRangeDropdown, { TimeRangeType } from './date-time-picker/TimeRangeDropdown';
import TimeRangeDisplay from './TimeRangeDisplay';

type Props = {
  className?: string,
  disabled?: boolean,
  hasErrorOnMount?: boolean,
  noOverride?: boolean,
  onChange: (nextTimeRange: TimeRange | NoTimeRangeOverride) => void,
  position?: 'bottom'|'right',
  validTypes?: Array<TimeRangeType>,
  value: TimeRange | NoTimeRangeOverride,
};

const FlexContainer = styled.span`
  display: flex;
  align-items: stretch;
  justify-content: space-between;
`;

const TimeRangeInput = ({ disabled, hasErrorOnMount, noOverride, value = {}, onChange, validTypes, position, className }: Props) => {
  const [show, setShow] = useState(false);

  if (validTypes && value && 'type' in value && !validTypes.includes(value?.type)) {
    throw new Error(`Value is of type ${value.type}, but only these types are valid: ${validTypes}`);
  }

  const toggleShow = () => setShow(!show);

  return (
    <FlexContainer className={className}>
      <TimeRangeDropdownButton disabled={disabled}
                               show={show}
                               toggleShow={toggleShow}
                               hasErrorOnMount={hasErrorOnMount}>
        <TimeRangeDropdown currentTimeRange={value}
                           noOverride={noOverride}
                           setCurrentTimeRange={onChange}
                           toggleDropdownShow={toggleShow}
                           validTypes={validTypes}
                           position={position} />
      </TimeRangeDropdownButton>
      <TimeRangeDisplay timerange={value} toggleDropdownShow={toggleShow} />
    </FlexContainer>
  );
};

TimeRangeInput.propTypes = {
  className: PropTypes.string,
  disabled: PropTypes.bool,
  hasErrorOnMount: PropTypes.bool,
  noOverride: PropTypes.bool,
  validTypes: PropTypes.arrayOf(PropTypes.string),
};

TimeRangeInput.defaultProps = {
  className: undefined,
  disabled: false,
  hasErrorOnMount: false,
  noOverride: false,
  validTypes: undefined,
  position: 'bottom',
};

export default TimeRangeInput;
