/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
// @flow strict
import * as React from 'react';
import { useCallback, useEffect, useRef } from 'react';
import PropTypes from 'prop-types';

import { DatePicker, Icon } from 'components/common';
import { Button, Tooltip } from 'components/graylog';
import DateTime from 'logic/datetimes/DateTime';
import { Input } from 'components/bootstrap';

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
  const inputRef = useRef(value);
  const _onChange = useCallback((newValue) => onChange({ target: { name, value: newValue } }), [name, onChange]);
  const _onChangeInput = useCallback((event) => _onChange(event.target.value), [_onChange]);
  const _onDatePicked = useCallback((date) => _onChange(_onDateSelected(date).toString(DateTime.Formats.DATE)), [_onChange]);
  const _onSetTimeToNow = () => _onChange(_setDateTimeToNow().toString(DateTime.Formats.TIMESTAMP));

  useEffect(() => {
    inputRef.current.input.value = value;
  }, [value]);

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
             onChange={_onChangeInput}
             placeholder={DateTime.Formats.DATETIME}
             ref={inputRef}
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
                  onChange={_onDatePicked} />
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
