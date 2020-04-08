// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import { useField } from 'formik';

import { ButtonToolbar, DropdownButton, MenuItem } from 'components/graylog';
import { Icon } from 'components/common';
import { migrateTimeRangeToNewType } from '../TimerangeForForm';

const TimeRangeOverrideTypeSelector = () => {
  const [{ value, onChange, name }] = useField('timerange');
  const { type } = value || {};
  const onSelect = useCallback((newType) => onChange({
    target: {
      value: migrateTimeRangeToNewType(value, newType),
      name,
    },
  }), [onChange, value]);
  return (
    <ButtonToolbar className="extended-search-timerange-chooser pull-left">
      <DropdownButton bsStyle="info"
                      title={<Icon name="clock"/>}
                      onSelect={onSelect}
                      id="dropdown-timerange-selector">
        <MenuItem eventKey="disabled"
                  active={type === undefined}>
          No Override
        </MenuItem>
        <MenuItem eventKey="relative"
                  active={type === 'relative'}>
          Relative
        </MenuItem>
        <MenuItem eventKey="absolute"
                  active={type === 'absolute'}>
          Absolute
        </MenuItem>
        <MenuItem eventKey="keyword"
                  active={type === 'keyword'}>
          Keyword
        </MenuItem>
      </DropdownButton>
    </ButtonToolbar>
  );
};

TimeRangeOverrideTypeSelector.propTypes = {};

export default TimeRangeOverrideTypeSelector;
