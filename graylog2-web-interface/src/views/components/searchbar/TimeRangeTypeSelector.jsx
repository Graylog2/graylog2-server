import React from 'react';
import { ButtonToolbar, DropdownButton, MenuItem } from 'components/graylog';
import { Icon } from 'components/common';

import PropTypes from 'views/components/CustomPropTypes';

export default function TimeRangeTypeSelector({ disabled, onSelect, value }) {
  return (
    <ButtonToolbar className="extended-search-timerange-chooser pull-left">
      <DropdownButton bsStyle="info"
                      disabled={disabled}
                      title={<Icon name="clock-o" />}
                      onSelect={onSelect}
                      id="dropdown-timerange-selector">
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
}

TimeRangeTypeSelector.propTypes = {
  disabled: PropTypes.bool,
  onSelect: PropTypes.func.isRequired,
  value: PropTypes.TimeRangeType.isRequired,
};

TimeRangeTypeSelector.defaultProps = {
  disabled: false,
};
