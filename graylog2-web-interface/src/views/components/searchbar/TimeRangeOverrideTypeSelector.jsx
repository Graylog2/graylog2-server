// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import { useField } from 'formik';

import { MenuItem, ButtonToolbar } from 'components/graylog';
import TimeRangeDropdownButton from 'views/components/searchbar/TimeRangeDropdownButton';
import { migrateTimeRangeToNewType } from '../TimerangeForForm';
import timeRangeTypeMenuItems from './TimeRangeTypeMenuItems';

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
      <TimeRangeDropdownButton onSelect={onSelect}>
        <MenuItem eventKey="disabled"
                  active={type === undefined}>
          No Override
        </MenuItem>
        {timeRangeTypeMenuItems(type)}
      </TimeRangeDropdownButton>
    </ButtonToolbar>
  );
};

export default TimeRangeOverrideTypeSelector;
