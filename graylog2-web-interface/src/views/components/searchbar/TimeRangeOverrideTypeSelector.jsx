import React from 'react';

import { ButtonToolbar, DropdownButton, MenuItem } from 'components/graylog';
import { Icon } from 'components/common';
import PropTypes from 'views/components/CustomPropTypes';

const TimeRangeOverrideTypeSelector = ({ onSelect, value }) => (
  <ButtonToolbar className="extended-search-timerange-chooser pull-left">
    <DropdownButton bsStyle="info"
                    title={<Icon name="clock-o" />}
                    onSelect={onSelect}
                    id="dropdown-timerange-selector">
      <MenuItem eventKey="disabled"
                active={value === undefined}>
        No Override
      </MenuItem>
      <MenuItem eventKey="relative"
                active={value === 'relative'}>
        Relative
      </MenuItem>
      <MenuItem eventKey="absolute"
                active={value === 'absolute'}>
        Absolute
      </MenuItem>
      <MenuItem eventKey="keyword"
                active={value === 'keyword'}>
        Keyword
      </MenuItem>
    </DropdownButton>
  </ButtonToolbar>
);

TimeRangeOverrideTypeSelector.propTypes = {
  onSelect: PropTypes.func.isRequired,
  value: PropTypes.TimeRangeType,
};

TimeRangeOverrideTypeSelector.defaultProps = {
  value: undefined,
};

export default TimeRangeOverrideTypeSelector;
