import React from 'react';
import { ButtonToolbar, DropdownButton, MenuItem } from 'react-bootstrap';

import PropTypes from 'views/components/CustomPropTypes';

const TimeRangeOverrideTypeSelector = ({ onSelect, value }) => (
  <ButtonToolbar className="extended-search-timerange-chooser pull-left">
    <DropdownButton bsStyle="info"
                    title={<i className="fa fa-clock-o" />}
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
  value: PropTypes.TimeRangeType.isRequired,
};

export default TimeRangeOverrideTypeSelector;
