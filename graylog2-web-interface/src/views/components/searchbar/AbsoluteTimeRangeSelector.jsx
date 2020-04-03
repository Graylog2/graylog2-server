// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { Field } from 'formik';

import DateTime from 'logic/datetimes/DateTime';
import { Icon } from 'components/common';
import DateInputWithPicker from './DateInputWithPicker';

import styles from './AbsoluteTimeRangeSelector.css';

type Props = {
  disabled: boolean,
};

const _isValidDateString = (dateString: string) => {
  if (!dateString) {
    return undefined;
  }
  return (DateTime.isValidDateString(dateString)
    ? undefined
    : `Invalid date: ${dateString}`);
};

const AbsoluteTimeRangeSelector = ({ disabled }: Props) => {
  return (
    <div className={`timerange-selector absolute ${styles.selectorContainer}`}>
      <Field name="timerange.from" validate={_isValidDateString}>
        {({ field: { value, onChange, onBlur, name }, meta: { error } }) => (
          <div className={styles.inputWidth}>
            <DateInputWithPicker disabled={disabled}
                                 onChange={onChange}
                                 onBlur={onBlur}
                                 value={value}
                                 name={name}
                                 title="Search start date"
                                 error={error} />
          </div>
        )}
      </Field>

      <p className={`text-center ${styles.separator}`}>
        <Icon name="long-arrow-right" />
      </p>

      <Field name="timerange.to" validate={_isValidDateString}>
        {({ field: { value, onChange, onBlur, name }, meta: { error } }) => (
          <div className={styles.inputWidth}>
            <DateInputWithPicker disabled={disabled}
                                 onChange={onChange}
                                 onBlur={onBlur}
                                 value={value}
                                 name={name}
                                 title="Search start date"
                                 error={error} />
          </div>
        )}
      </Field>
    </div>
  );
};

AbsoluteTimeRangeSelector.propTypes = {
  disabled: PropTypes.bool,
};

AbsoluteTimeRangeSelector.defaultProps = {
  disabled: false,
};

export default AbsoluteTimeRangeSelector;
