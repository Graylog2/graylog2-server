// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import { useField } from 'formik';

import { MenuItem, ButtonToolbar } from 'components/graylog';
import TimeRangeDropdownButton from 'views/components/searchbar/TimeRangeDropdownButton';

import Dropdown from './date-time-picker/Dropdown';

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
    <ButtonToolbar className="pull-left">
      <TimeRangeDropdownButton>
        <MenuItem eventKey="disabled"
                  active={type === undefined}>
          No Override
        </MenuItem>
        <Dropdown currentType={type} onSelect={onSelect} />
      </TimeRangeDropdownButton>
    </ButtonToolbar>
  );
};

export default TimeRangeOverrideTypeSelector;
