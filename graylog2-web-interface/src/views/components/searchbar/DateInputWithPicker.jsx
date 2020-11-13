// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';

import { DatePicker, Icon } from 'components/common';
import { Button, Tooltip } from 'components/graylog';
import DateTime from 'logic/datetimes/DateTime';
import { Input } from 'components/bootstrap';

type Props = {
  disabled: ?boolean,
  error: ?string,
  value: string,
  onChange: (string) => void,
  name: string,
  title: ?string,
  initialDateTimeObject: {
    years: string | number,
    months: string | number,
    date: string | number,
    hours: string | number,
    minutes: string | number,
    seconds: string | number,
    milliseconds: string | number,
  },
};

const DateInputWithPicker = ({ disabled = false, error, value, onChange, name, title, initialDateTimeObject }: Props) => {
  const _onDatePicked = (date) => {
    const newDate = moment(date).toObject();

    return onChange(moment({
      ...initialDateTimeObject,
      years: newDate.years,
      months: newDate.months,
      date: newDate.date,
    }).format(DateTime.Formats.TIMESTAMP));
  };

  const _onChangeInput = (event) => onChange(event.target.value);
  const _onSetTimeToNow = () => onChange(moment().format(DateTime.Formats.TIMESTAMP));

  return (
    <div>
      {error && (
        <Tooltip placement="top" className="in" id="tooltip-top" positionTop="20px">
          {error}
        </Tooltip>
      )}

      <Input type="text"
             id={`date-input-${name}`}
             name={name}
             autoComplete="off"
             disabled={disabled}
             onChange={_onChangeInput}
             placeholder={DateTime.Formats.DATETIME}
             value={value}
             buttonAfter={(
               <Button disabled={disabled}
                       onClick={_onSetTimeToNow}
                       title="Insert current date">
                 <Icon name="magic" />
               </Button>
             )}
             bsStyle={error ? 'error' : null} />

      <DatePicker id={`date-input-datepicker-${name}`}
                  disabled={disabled}
                  title={title}
                  date={value}
                  onChange={_onDatePicked}
                  fromDate={fromDate} />
    </div>
  );
};

DateInputWithPicker.propTypes = {
  disabled: PropTypes.bool,
  error: PropTypes.string,
  value: PropTypes.oneOfType([PropTypes.object, PropTypes.string]),
  onChange: PropTypes.func.isRequired,
  name: PropTypes.string.isRequired,
  title: PropTypes.string,
  initialDateTimeObject: PropTypes.shape({
    years: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    months: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    date: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    hours: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    minutes: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    seconds: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    milliseconds: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  }).isRequired,
};

DateInputWithPicker.defaultProps = {
  disabled: false,
  error: undefined,
  value: undefined,
  title: '',
};

export default DateInputWithPicker;
