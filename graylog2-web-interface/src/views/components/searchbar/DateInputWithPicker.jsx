// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import PropTypes from 'prop-types';

import { DatePicker, Icon } from 'components/common';
import { Button, Tooltip } from 'components/graylog';
import Input from 'components/bootstrap/Input';
import DateTime from 'logic/datetimes/DateTime';

const _setDateTimeToNow = () => new DateTime();

const _onDateSelected = (date) => {
  const midnightDate = date.setHours(0);

  return DateTime.ignoreTZ(midnightDate);
};

type Props = {
  disabled: ?boolean,
  error: ?string,
  value: string,
  onBlur: ?(({}) => void),
  onChange: ({ target: { name: string, value: string } }) => void,
  name: string,
  title: ?string,
};
const DateInputWithPicker = ({ disabled = false, error, value, onBlur = () => {}, onChange, name, title }: Props) => {
  const _onChange = useCallback((newValue) => onChange({ target: { name, value: newValue } }), [onChange]);
  const onDatePicked = useCallback((date) => _onChange(_onDateSelected(date).toString(DateTime.Formats.TIMESTAMP)), [_onChange]);
  const onSetTimeToNow = useCallback(() => _onChange(_setDateTimeToNow().toString(DateTime.Formats.TIMESTAMP)), [_onChange]);

  return (
    <div>
      {error && (
        <Tooltip placement="top" className="in" id="tooltip-top" positionTop="-30px">
          {error}
        </Tooltip>
      )}
      <DatePicker id={`date-input-datepicker-${name}`}
                  disabled={disabled}
                  title={title}
                  date={value}
                  onChange={onDatePicked}>
        <Input type="text"
               id={`date-input-${name}`}
               name={name}
               autoComplete="off"
               disabled={disabled}
               className="absolute"
               value={value}
               onBlur={onBlur}
               onChange={onChange}
               placeholder={DateTime.Formats.DATETIME}
               buttonAfter={(
                 <Button disabled={disabled} onClick={onSetTimeToNow} title="Insert current date">
                   <Icon name="magic" />
                 </Button>
             )}
               bsStyle={error ? 'error' : null}
               required />
      </DatePicker>
    </div>
  );
};

DateInputWithPicker.propTypes = {
  disabled: PropTypes.bool,
  error: PropTypes.string,
  value: PropTypes.oneOfType([PropTypes.object, PropTypes.string]),
  onBlur: PropTypes.func,
  onChange: PropTypes.func.isRequired,
  name: PropTypes.string.isRequired,
  title: PropTypes.string,
};

DateInputWithPicker.defaultProps = {
  disabled: false,
  onBlur: () => {},
  error: undefined,
  value: undefined,
  title: '',
};

export default DateInputWithPicker;
