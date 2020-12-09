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

import TimeRangeDropdownButton from 'views/components/searchbar/TimeRangeDropdownButton';

import TimeRangeDropdown from './date-time-picker/TimeRangeDropdown';

type Props = {
  disabled?: boolean,
  noOverride?: boolean,
};

const TimeRangeTypeSelector = ({ disabled, noOverride }: Props) => {
  const [show, setShow] = useState(false);

  const toggleShow = () => setShow(!show);

  return (
    <TimeRangeDropdownButton disabled={disabled}
                             show={show}
                             toggleShow={toggleShow}>
      <TimeRangeDropdown toggleDropdownShow={toggleShow}
                         noOverride={noOverride} />
    </TimeRangeDropdownButton>
  );
};

TimeRangeTypeSelector.propTypes = {
  disabled: PropTypes.bool,
  noOverride: PropTypes.bool,
};

TimeRangeTypeSelector.defaultProps = {
  disabled: false,
  noOverride: false,
};

export default TimeRangeTypeSelector;
