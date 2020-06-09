// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import { useField } from 'formik';

import TimeRangeDropdownButton from 'views/components/searchbar/TimeRangeDropdownButton';
import { ButtonToolbar } from 'components/graylog';

import PropTypes from 'views/components/CustomPropTypes';
import { migrateTimeRangeToNewType } from '../TimerangeForForm';
import timeRangeTypeMenuItems from './TimeRangeTypeMenuItems';

type Props = {
  disabled: boolean,
};

export default function TimeRangeTypeSelector({ disabled }: Props) {
  const [{ value, onChange, name }] = useField('timerange');
  const { type: currentType } = value;
  const onSelect = useCallback((newType) => onChange({
    target: {
      value: migrateTimeRangeToNewType(value, newType),
      name,
    },
  }), [onChange, value]);
  return (
    <ButtonToolbar className="pull-left">
      <TimeRangeDropdownButton disabled={disabled} onSelect={onSelect}>
        {timeRangeTypeMenuItems(currentType)}
      </TimeRangeDropdownButton>
    </ButtonToolbar>
  );
}

TimeRangeTypeSelector.propTypes = {
  disabled: PropTypes.bool,
};

TimeRangeTypeSelector.defaultProps = {
  disabled: false,
};
