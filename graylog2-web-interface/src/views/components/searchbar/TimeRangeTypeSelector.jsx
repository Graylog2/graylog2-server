import React from 'react';
import { ButtonToolbar, DropdownButton, MenuItem, Icon } from 'components/graylog';

import PropTypes from 'views/components/CustomPropTypes';

export default function TimeRangeTypeSelector({ onSelect, value }) {
  return (
    <ButtonToolbar className="extended-search-timerange-chooser pull-left">
      <DropdownButton bsStyle="info"
                      title={<Icon className="fa fa-clock-o" />}
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
  onSelect: PropTypes.func.isRequired,
  value: PropTypes.TimeRangeType.isRequired,
};
