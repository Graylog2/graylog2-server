// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import { useField } from 'formik';

import { ButtonToolbar, DropdownButton, MenuItem } from 'components/graylog';
import { Icon } from 'components/common';

import PropTypes from 'views/components/CustomPropTypes';
import { migrateTimeRangeToNewType } from '../TimerangeForForm';

type Props = {
  disabled: boolean,
};

export default function TimeRangeTypeSelector({ disabled }: Props) {
  const [{ value, onChange, name }] = useField('timerange');
  const { type } = value;
  const onSelect = useCallback((newType) => onChange({
    target: {
      value: migrateTimeRangeToNewType(value, newType),
      name,
    },
  }), [onChange, value]);
  return (
    <ButtonToolbar className="extended-search-timerange-chooser pull-left">
      <DropdownButton bsStyle="info"
                      id="timerange-type"
                      disabled={disabled}
                      title={<Icon name="clock-o" />}
                      onSelect={onSelect}>
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
}

TimeRangeTypeSelector.propTypes = {
  disabled: PropTypes.bool,
};

TimeRangeTypeSelector.defaultProps = {
  disabled: false,
};
