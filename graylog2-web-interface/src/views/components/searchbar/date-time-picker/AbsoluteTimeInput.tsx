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
import { useRef } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import moment from 'moment';

import { Icon } from 'components/common';
import { Button, FormGroup, InputGroup, FormControl } from 'components/graylog';
import DateTime from 'logic/datetimes/DateTime';

const TIME_ICON_BOD = 'hourglass-start';
const TIME_ICON_MID = 'hourglass-half';
const TIME_ICON_EOD = 'hourglass-end';

const TIME_TYPES = [
  'hours',
  'minutes',
  'seconds',
  'milliseconds',
];

const SetTimeOption = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;

  b { padding: 0 3px; }
`;

const StyledFormControl = styled(FormControl)`
  padding: 0 6px 0 9px;
  width: 50px !important;
  
  &:last-of-type {
    width: 60px !important;
  }
`;

const StyledInputAddon = styled(InputGroup.Addon)(({ theme }) => css`
  padding: 0;
  background: ${theme.colors.variant.lightest.default};
  font-weight: bold;

  &:not(:first-child):not(:last-child) {
    border-right: 0;
    border-left: 0;
    padding: 0 3px;
  }
`);

const StyledButton = styled(Button)`
  padding: 6px 9px;
  line-height: 1.1;
`;

const _onFocusSelect = (event) => {
  event.target.select();
};

const zeroPad = (data, pad = 2) => String(data).padStart(pad, '0');

const parseTimeValue = (value, type) => {
  const isNotNumeric = value.match(/[^0-9]/g);

  const timeValue = Number(isNotNumeric ? 0 : value);

  if (type === 'hours') {
    if (timeValue > 23) {
      return 23;
    }
  } else if (type === 'milliseconds') {
    if (timeValue > 999) {
      return 999;
    }
  } else if (timeValue > 59) {
    return 59;
  }

  return timeValue;
};

const fieldUpdate = (value) => {
  const initialDateTime = moment(value).toObject();

  TIME_TYPES.forEach((type) => {
    initialDateTime[type] = zeroPad(initialDateTime[type], type === 'milliseconds' ? 3 : 2);
  });

  const handleChangeSetTime = (event) => {
    const timeType = event.target.getAttribute('id').split('-').pop();
    const timeValue = parseTimeValue(event.target.value, timeType);

    const newTime = moment({
      ...initialDateTime,
      [timeType]: timeValue,
    });

    return newTime.format(DateTime.Formats.TIMESTAMP);
  };

  const handleClickTimeNow = () => {
    const newTime = moment().toObject();

    return moment({
      ...initialDateTime,
      hours: newTime.hours,
      minutes: newTime.minutes,
      seconds: newTime.seconds,
      milliseconds: newTime.milliseconds,
    }).format(DateTime.Formats.TIMESTAMP);
  };

  const handleTimeToggle = (eod = false) => {
    return moment({
      ...initialDateTime,
      hours: eod ? 23 : 0,
      minutes: eod ? 59 : 0,
      seconds: eod ? 59 : 0,
      milliseconds: eod ? 999 : 0,
    }).format(DateTime.Formats.TIMESTAMP);
  };

  return {
    initialDateTime,
    handleChangeSetTime,
    handleClickTimeNow,
    handleTimeToggle,
  };
};

const AbsoluteTimeInput = ({ dateTime, range, onChange }) => {
  const hourIcon = useRef(TIME_ICON_MID);

  const {
    initialDateTime,
    handleChangeSetTime,
    handleClickTimeNow,
    handleTimeToggle,
  } = fieldUpdate(dateTime);

  const _onChangeSetTime = (event) => {
    hourIcon.current = TIME_ICON_MID;

    onChange(handleChangeSetTime(event));
  };

  const _onClickHourToggle = () => {
    const endOfDay = hourIcon.current === TIME_ICON_BOD;

    if (endOfDay) {
      hourIcon.current = TIME_ICON_EOD;
    } else {
      hourIcon.current = TIME_ICON_BOD;
    }

    onChange(handleTimeToggle(endOfDay));
  };

  const _onClickTimeNow = () => {
    hourIcon.current = TIME_ICON_MID;

    onChange(handleClickTimeNow());
  };

  return (
    <SetTimeOption>
      <FormGroup>
        <InputGroup>
          <StyledInputAddon>
            <StyledButton bsStyle="link"
                          bsSize="small"
                          onClick={_onClickHourToggle}
                          title="Toggle between beginning and end of day">
              <Icon name={hourIcon.current} />
            </StyledButton>
          </StyledInputAddon>
          <StyledFormControl type="number"
                             id={`${range}-time-hours`}
                             title={`${range} hour`}
                             value={initialDateTime.hours ?? ''}
                             onChange={_onChangeSetTime}
                             onFocus={_onFocusSelect}
                             bsSize="sm" />
          <StyledInputAddon>:</StyledInputAddon>
          <StyledFormControl type="number"
                             id={`${range}-time-minutes`}
                             title={`${range} minutes`}
                             value={initialDateTime.minutes ?? ''}
                             onChange={_onChangeSetTime}
                             onFocus={_onFocusSelect}
                             bsSize="sm" />
          <StyledInputAddon>:</StyledInputAddon>
          <StyledFormControl type="number"
                             id={`${range}-time-seconds`}
                             title={`${range} seconds`}
                             value={initialDateTime.seconds ?? ''}
                             onChange={_onChangeSetTime}
                             onFocus={_onFocusSelect}
                             bsSize="sm" />
          <StyledInputAddon>.</StyledInputAddon>
          <StyledFormControl type="number"
                             id={`${range}-time-milliseconds`}
                             title={`${range} milliseconds`}
                             value={initialDateTime.milliseconds ?? ''}
                             onChange={_onChangeSetTime}
                             onFocus={_onFocusSelect}
                             bsSize="sm" />
          <StyledInputAddon>
            <StyledButton bsStyle="link"
                          bsSize="small"
                          onClick={_onClickTimeNow}
                          title="Set to current local time">
              <Icon name="magic" />
            </StyledButton>
          </StyledInputAddon>
        </InputGroup>
      </FormGroup>
    </SetTimeOption>
  );
};

AbsoluteTimeInput.propTypes = {
  dateTime: PropTypes.string.isRequired,
  range: PropTypes.string.isRequired,
  onChange: PropTypes.func,
};

AbsoluteTimeInput.defaultProps = {
  onChange: () => {},
};

export default AbsoluteTimeInput;
