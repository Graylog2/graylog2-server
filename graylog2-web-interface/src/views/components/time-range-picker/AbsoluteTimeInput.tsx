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
import styled, { css } from 'styled-components';
import moment from 'moment';
import type { Moment } from 'moment';

import { Icon } from 'components/common';
import { Button, FormGroup, InputGroup, FormControl } from 'components/bootstrap';
import type { IconName } from 'components/common/Icon';
import { DATE_TIME_FORMATS } from 'util/DateTime';
import useUserDateTime from 'hooks/useUserDateTime';

const TIME_ICON_BOD = 'hourglass_top';
const TIME_ICON_MID = 'hourglass';
const TIME_ICON_EOD = 'hourglass_bottom';

const TIME_TYPES = ['hours', 'minutes', 'seconds', 'milliseconds'];

const SetTimeOption = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  padding-top: 12px;

  b {
    padding: 0 3px;
  }
`;

const StyledFormControl = styled(FormControl)`
  padding: 0 6px 0 9px;

  &:nth-of-type(1) {
    grid-area: 2 / 2 / 2 / 2;
  }

  &:nth-of-type(2) {
    grid-area: 2 / 4 / 2 / 4;
  }

  &:nth-of-type(3) {
    grid-area: 2 / 6 / 2 / 6;
  }
`;

const StyledInputAddon = styled(InputGroup.Addon)(
  ({ theme }) => css`
    padding: 0;
    background: ${theme.colors.variant.lightest.default};
    font-weight: bold;
    width: auto;
    display: flex;
    align-items: center;

    &:not(:first-child, :last-child) {
      border-right: 0;
      border-left: 0;
      padding: 0 3px;
    }

    &:nth-of-type(1) {
      grid-area: 2 / 1 / 2 / 1;
    }

    &:nth-of-type(2) {
      grid-area: 2 / 3 / 2 / 3;
    }

    &:nth-of-type(3) {
      grid-area: 2 / 5 / 2 / 5;
    }

    &:nth-of-type(4) {
      grid-area: 2 / 7 / 2 / 7;
    }
  `,
);

const StyledButton = styled(Button)`
  padding: 6px 9px;
  line-height: 1;
`;

const FormGroupGrid = styled(FormGroup)`
  display: grid;
  grid-template-columns: max-content repeat(3, 1fr max-content);
  grid-template-rows: auto 1fr;

  label {
    padding-left: 6px;
    margin: 0;

    &:nth-child(1) {
      grid-area: 1 / 2 / 1 / 2;
    }

    &:nth-child(2) {
      grid-area: 1 / 4 / 1 / 4;
    }

    &:nth-child(3) {
      grid-area: 1 / 6 / 1 / 6;
    }
  }
`;

const GridInputGroup = styled(InputGroup)`
  display: contents;
`;

const onFocusSelect = (event) => {
  event.target.select();
};

const zeroPad = (data, pad = 2) => String(data).padStart(pad, '0');

const parseTimeValue = (value: string, type: string) => {
  const isNotNumeric = value.match(/[^0-9]/g);

  const timeValue = Number(isNotNumeric ? 0 : value);

  if (type === 'hours') {
    if (timeValue > 23) {
      return 23;
    }
  } else if (timeValue > 59) {
    return 59;
  }

  return timeValue;
};

const fieldUpdate = (value: string, toUserTimezone: (date: Date) => Moment) => {
  const initialDateTime = moment(value).toObject();

  TIME_TYPES.forEach((type) => {
    initialDateTime[type] = zeroPad(initialDateTime[type]);
  });

  const handleChangeSetTime = (event) => {
    const timeType = event.target.getAttribute('id').split('-').pop();
    const timeValue = parseTimeValue(event.target.value, timeType);

    const newTime = moment({
      ...initialDateTime,
      [timeType]: timeValue,
    });

    return newTime.format(DATE_TIME_FORMATS.default);
  };

  const handleClickTimeNow = (disableMinute: boolean, disableSecond: boolean) => {
    const newTime = toUserTimezone(new Date()).toObject();

    return moment({
      ...initialDateTime,
      hours: newTime.hours,
      minutes: !disableMinute && newTime.minutes,
      seconds: !disableSecond && newTime.seconds,
    }).format(DATE_TIME_FORMATS.default);
  };

  const handleTimeToggle = (disableMinute: boolean, disableSecond: boolean, eod = false) =>
    moment({
      ...initialDateTime,
      hours: eod ? 23 : 0,
      minutes: eod && !disableMinute ? 59 : 0,
      seconds: eod && !disableSecond ? 59 : 0,
    }).format(DATE_TIME_FORMATS.default);

  return {
    initialDateTime,
    handleChangeSetTime,
    handleClickTimeNow,
    handleTimeToggle,
  };
};

type AbsoluteTimeInputProps = {
  dateTime: string;
  range: string;
  onChange: (time: string) => void;
  disableMinute?: boolean;
  disableSecond?: boolean;
};

const AbsoluteTimeInput = ({
  dateTime,
  range,
  onChange,
  disableMinute = false,
  disableSecond = false,
}: AbsoluteTimeInputProps) => {
  const hourIcon = useRef<IconName>(TIME_ICON_MID);
  const { toUserTimezone } = useUserDateTime();

  const { initialDateTime, handleChangeSetTime, handleClickTimeNow, handleTimeToggle } = fieldUpdate(
    dateTime,
    toUserTimezone,
  );

  const onChangeSetTime = (event) => {
    hourIcon.current = TIME_ICON_MID;

    if (onChange) {
      onChange(handleChangeSetTime(event));
    }
  };

  const onClickHourToggle = () => {
    const endOfDay = hourIcon.current === TIME_ICON_BOD;

    if (endOfDay) {
      hourIcon.current = TIME_ICON_EOD;
    } else {
      hourIcon.current = TIME_ICON_BOD;
    }

    if (onChange) {
      onChange(handleTimeToggle(disableMinute, disableSecond, endOfDay));
    }
  };

  const onClickTimeNow = () => {
    hourIcon.current = TIME_ICON_MID;

    if (onChange) {
      onChange(handleClickTimeNow(disableMinute, disableSecond));
    }
  };

  return (
    <SetTimeOption>
      <FormGroupGrid>
        <label htmlFor={`${range}-time-hours`} title={`${range} hours label`}>
          HH
        </label>
        <label htmlFor={`${range}-time-minutes`} title={`${range} minutes label`}>
          mm
        </label>
        <label htmlFor={`${range}-time-seconds`} title={`${range} seconds label`}>
          ss
        </label>
        <GridInputGroup>
          <StyledInputAddon>
            <StyledButton
              bsStyle="link"
              bsSize="small"
              onClick={onClickHourToggle}
              title="Toggle between beginning and end of day">
              <Icon name={hourIcon.current} />
            </StyledButton>
          </StyledInputAddon>
          <StyledFormControl
            type="number"
            id={`${range}-time-hours`}
            title={`${range} hour`}
            value={initialDateTime.hours ?? ''}
            onChange={onChangeSetTime}
            onFocus={onFocusSelect}
          />
          <StyledInputAddon>:</StyledInputAddon>
          <StyledFormControl
            type="number"
            id={`${range}-time-minutes`}
            title={`${range} minutes`}
            value={initialDateTime.minutes ?? ''}
            onChange={onChangeSetTime}
            disabled={disableMinute}
            onFocus={onFocusSelect}
          />
          <StyledInputAddon>:</StyledInputAddon>
          <StyledFormControl
            type="number"
            id={`${range}-time-seconds`}
            title={`${range} seconds`}
            value={initialDateTime.seconds ?? ''}
            onChange={onChangeSetTime}
            disabled={disableSecond}
            onFocus={onFocusSelect}
          />
          <StyledInputAddon>
            <StyledButton bsStyle="link" bsSize="small" onClick={onClickTimeNow} title="Set to current local time">
              <Icon name="calendar_clock" />
            </StyledButton>
          </StyledInputAddon>
        </GridInputGroup>
      </FormGroupGrid>
    </SetTimeOption>
  );
};

export default AbsoluteTimeInput;
