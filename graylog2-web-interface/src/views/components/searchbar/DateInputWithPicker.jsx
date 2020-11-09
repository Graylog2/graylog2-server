// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import PropTypes from 'prop-types';

import { DatePicker, Icon } from 'components/common';
import { Button, Tooltip } from 'components/graylog';
import DateTime from 'logic/datetimes/DateTime';

import { Input } from '../../../components/bootstrap';

const _onDateSelected = (date) => {
  const midnightDate = date.setHours(0);

  return DateTime.ignoreTZ(midnightDate);
};

type Props = {
  disabled: ?boolean,
  error: ?string,
  value: string,
  onChange: ({ target: { name: string, value: string } }) => void,
  name: string,
  title: ?string,
};

const _setDateTimeToNow = () => new DateTime();

const DateInputWithPicker = ({ disabled = false, error, value, onChange, name, title }: Props) => {
  const _onChange = useCallback((newValue) => onChange({ target: { name, value: newValue } }), [name, onChange]);
  const onDatePicked = useCallback((date) => _onChange(_onDateSelected(date).toString(DateTime.Formats.DATE)), [_onChange]);
  const _onSetTimeToNow = () => onChange({ target: { name, value: _setDateTimeToNow().toString(DateTime.Formats.TIMESTAMP) } });

  return (
    <div>
      {error && (
        <Tooltip placement="top" className="in" id="tooltip-top" positionTop="-30px">
          {error}
        </Tooltip>
      )}

      <Input type="text"
             id={`date-input-${name}`}
             name={name}
             autoComplete="off"
             disabled={disabled}
             className="absolute"
             value={value}
             onChange={onChange}
             placeholder={DateTime.Formats.DATETIME}
             buttonAfter={(
               <Button disabled={disabled}
                       onClick={_onSetTimeToNow}
                       title="Insert current date">
                 <Icon name="magic" />
               </Button>
             )}
             bsStyle={error ? 'error' : null}
             required />

      <DatePicker id={`date-input-datepicker-${name}`}
                  disabled={disabled}
                  title={title}
                  date={value}
                  onChange={onDatePicked} />
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
};

DateInputWithPicker.defaultProps = {
  disabled: false,
  error: undefined,
  value: undefined,
  title: '',
};

export default DateInputWithPicker;
