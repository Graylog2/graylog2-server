// @flow strict
import * as React from 'react';
import { useCallback, useState } from 'react';
import { useFormikContext } from 'formik';
import PropTypes from 'prop-types';

import TimeRangeDropdownButton from 'views/components/searchbar/TimeRangeDropdownButton';

import Dropdown from './date-time-picker/Dropdown';

import { migrateTimeRangeToNewType } from '../TimerangeForForm';

type Props = {
  config: any,
  disabled: boolean,
};

export default function TimeRangeTypeSelector({ config, disabled }: Props) {
  const [show, setShow] = useState(false);
  const formik = useFormikContext();
  const { value, onChange, name } = formik.getFieldProps('timerange');
  const { type: currentType } = value;
  const onSelect = useCallback((newType) => onChange({
    target: {
      value: migrateTimeRangeToNewType(value, newType),
      name,
    },
  }), [onChange, value]);
  const toggleShow = () => setShow(!show);

  return (
    <TimeRangeDropdownButton disabled={disabled}
                             show={show}
                             toggleShow={toggleShow}>
      <Dropdown currentType={currentType}
                onSelect={onSelect}
                config={config}
                toggleDropdownShow={toggleShow} />
    </TimeRangeDropdownButton>
  );
}

TimeRangeTypeSelector.propTypes = {
  disabled: PropTypes.bool,
};

TimeRangeTypeSelector.defaultProps = {
  disabled: false,
};
