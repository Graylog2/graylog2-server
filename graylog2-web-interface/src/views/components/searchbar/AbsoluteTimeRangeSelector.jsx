// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { Field } from 'formik';

import { Icon } from 'components/common';
import DateTime from 'logic/datetimes/DateTime';
import DateInputWithPicker from './DateInputWithPicker';

import styles from './AbsoluteTimeRangeSelector.css';

const _isValidDateString = (dateString) => {
  try {
    if (dateString !== undefined) {
      DateTime.parseFromString(dateString);
    }
    return undefined;
  } catch (e) {
    return `Invalid date: ${e}`;
  }
};

type Props = {
  disabled: boolean,
};

const AbsoluteTimeRangeSelector = ({ disabled }: Props) => (
  <div className={`timerange-selector absolute ${styles.selectorContainer}`}>
    <div className={styles.inputWidth}>
      <Field name="timerange.from" validate={_isValidDateString}>
        {({ field: { value, onChange, onBlur, name }, meta: { error } }) => (
          <DateInputWithPicker disabled={disabled}
                               onChange={onChange}
                               onBlur={onBlur}
                               value={value}
                               name={name}
                               title="Search start date"
                               error={error} />
        )}
      </Field>
    </div>

    <p className={`text-center ${styles.separator}`}>
      <Icon name="long-arrow-right" />
    </p>

    <div className={styles.inputWidth}>
      <Field name="timerange.to" validate={_isValidDateString}>
        {({ field: { value, onChange, onBlur, name }, meta: { error } }) => (
          <DateInputWithPicker disabled={disabled}
                               onChange={onChange}
                               onBlur={onBlur}
                               value={value}
                               name={name}
                               title="Search start date"
                               error={error} />
        )}
      </Field>
    </div>
  </div>
);

AbsoluteTimeRangeSelector.propTypes = {
  disabled: PropTypes.bool,
};

AbsoluteTimeRangeSelector.defaultProps = {
  disabled: false,
};

export default AbsoluteTimeRangeSelector;
