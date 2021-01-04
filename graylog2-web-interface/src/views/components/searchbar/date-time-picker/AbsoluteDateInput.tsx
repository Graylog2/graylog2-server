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
import * as React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';

import DateTime from 'logic/datetimes/DateTime';
import { Icon } from 'components/common';
import { Button } from 'components/graylog';
import { Input } from 'components/bootstrap';

const AbsoluteDateInput = ({ name, disabled, onChange, value, hasError }) => {
  const _onSetTimeToNow = () => onChange(moment().format(DateTime.Formats.TIMESTAMP));
  const _onChange = (event) => onChange(event.target.value);

  return (
    <Input type="text"
           id={`date-input-${name}`}
           name={name}
           autoComplete="off"
           disabled={disabled}
           onChange={_onChange}
           placeholder={DateTime.Formats.DATETIME}
           value={value}
           buttonAfter={(
             <Button disabled={disabled}
                     onClick={_onSetTimeToNow}
                     title="Insert current date">
               <Icon name="magic" />
             </Button>
           )}
           className="mousetrap"
           bsStyle={hasError ? 'error' : null} />
  );
};

AbsoluteDateInput.propTypes = {
  name: PropTypes.string.isRequired,
  disabled: PropTypes.bool,
  onChange: PropTypes.func,
  value: PropTypes.string,
  hasError: PropTypes.bool,
};

AbsoluteDateInput.defaultProps = {
  disabled: false,
  onChange: () => {},
  value: '',
  hasError: false,
};

export default AbsoluteDateInput;
