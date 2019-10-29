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
                className={value === undefined ? 'selected' : null}>
        No Override
      </MenuItem>
      <MenuItem eventKey="relative"
                className={value === 'relative' ? 'selected' : null}>
        Relative
      </MenuItem>
      <MenuItem eventKey="absolute"
                className={value === 'absolute' ? 'selected' : null}>
        Absolute
      </MenuItem>
      <MenuItem eventKey="keyword"
                className={value === 'keyword' ? 'selected' : null}>
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
