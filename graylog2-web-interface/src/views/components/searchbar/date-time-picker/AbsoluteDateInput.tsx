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
import styled from 'styled-components';

import { Icon } from 'components/common';
import { Button, Input } from 'components/bootstrap';
import { DATE_TIME_FORMATS } from 'util/DateTime';
import useUserDateTime from 'hooks/useUserDateTime';

const Wrapper = styled.div`
  margin: 9px 6px;
  width: 100%;
  
  .form-group {
    margin: 0;
  }
`;

const AbsoluteDateInput = ({ name, disabled, onChange, value, hasError }) => {
  const { formatTime } = useUserDateTime();
  const _onSetTimeToNow = () => onChange(formatTime(new Date(), 'complete'));
  const _onChange = (event) => onChange(event.target.value);

  return (
    <Wrapper>
      <Input type="text"
             id={`date-input-${name}`}
             name={name}
             autoComplete="off"
             disabled={disabled}
             onChange={_onChange}
             placeholder={DATE_TIME_FORMATS.default}
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
    </Wrapper>
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
