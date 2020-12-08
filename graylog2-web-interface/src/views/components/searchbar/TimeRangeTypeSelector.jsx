// @flow strict
import * as React from 'react';
import { useState } from 'react';
import PropTypes from 'prop-types';

import TimeRangeDropdownButton from 'views/components/searchbar/TimeRangeDropdownButton';

import TimeRangeDropdown from './date-time-picker/TimeRangeDropdown';

type Props = {
  config: any,
  disabled?: boolean,
  noOverride?: boolean,
};

const TimeRangeTypeSelector = ({ config, disabled, noOverride }: Props) => {
  const [show, setShow] = useState(false);

  const toggleShow = () => setShow(!show);

  return (
    <TimeRangeDropdownButton disabled={disabled}
                             show={show}
                             toggleShow={toggleShow}>
      <TimeRangeDropdown config={config}
                         toggleDropdownShow={toggleShow}
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
