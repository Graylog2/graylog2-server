// @flow strict
import * as React from 'react';
import { useCallback } from 'react';

import { DatePicker, Icon } from 'components/common';
import { Button } from 'components/graylog';
import Input from 'components/bootstrap/Input';
import DateTime from 'logic/datetimes/DateTime';

const _setDateTimeToNow = () => new DateTime();

const _onDateSelected = (date) => {
  const midnightDate = date.setHours(0);
  return DateTime.ignoreTZ(midnightDate);
};

type Props = {
  disabled: boolean,
  error: ?string,
  value: string,
  onBlur: ({}) => void,
  onChange: ({ target: { name: string, value: string } }) => void,
  name: string,
  title: string,
};
const DateInputWithPicker = ({ disabled, error, value, onBlur, onChange, name, title }: Props) => {
  const _onChange = useCallback((newValue) => onChange({ target: { name, value: newValue } }), [onChange]);
  const onDatePicked = useCallback((date) => _onChange(_onDateSelected(date)), [_onChange]);
  const onSetTimeToNow = useCallback(() => _onChange(_setDateTimeToNow()), [_onChange]);
  return (
    <DatePicker disabled={disabled}
                title={title}
                date={value.toString()}
                onChange={onDatePicked}>
      <Input type="text"
             name={name}
             disabled={disabled}
             className="absolute"
             value={value.toString()}
             onBlur={onBlur}
             onChange={onChange}
             placeholder={DateTime.Formats.DATETIME}
             buttonAfter={(
               <Button disabled={disabled} onClick={onSetTimeToNow}>
                 <Icon name="magic" />
               </Button>
             )}
             bsStyle={error ? 'error' : null}
             required />
    </DatePicker>
  );
};

DateInputWithPicker.propTypes = {};

export default DateInputWithPicker;
