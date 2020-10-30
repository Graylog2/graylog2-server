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
import { useRef } from 'react';
import PropTypes from 'prop-types';
import { Field } from 'formik';
import styled, { css } from 'styled-components';
import type { StyledComponent } from 'styled-components';
import moment from 'moment';

import DateTime from 'logic/datetimes/DateTime';
import { Icon } from 'components/common';
import {
  Button,
  FormGroup,
  InputGroup,
  FormControl,
} from 'components/graylog';
import DateInputWithPicker from 'views/components/searchbar/DateInputWithPicker';
import type { ThemeInterface } from 'theme';

type Props = {
  disabled: boolean,
  originalTimeRange: {
    from: string,
    top: string,
  },
};

const TIME_TYPES = [
  'hours',
  'minutes',
  'seconds',
  'milliseconds',
];

const AbsoluteWrapper: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  display: flex;
  align-items: stretch;
  justify-content: space-around;
`;

const RangeWrapper: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  flex: 4;
  align-items: center;
  min-height: 290px;
`;

const IconWrap: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  flex: 0.75;
  display: flex;
  align-items: center;
  justify-content: center;
`;

const SetTimeOption: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;

  b { padding: 0 3px; }
`;

const StyledInputAddon: StyledComponent<{}, ThemeInterface, typeof InputGroup.Addon> = styled(InputGroup.Addon)(({ theme }) => css`
  padding: 0;
  background: ${theme.colors.variant.lightest.default};

  &:not(:first-child):not(:last-child) {
    border-right: 0;
    border-left: 0;
    padding: 0 3px;
  }
`);

const StyledFormControl: StyledComponent<{}, void, typeof FormControl> = styled(FormControl)`
  padding: 0 9px;
`;

const StyledButton = styled(Button)`
  padding: 6px 9px;
  line-height: 1.1;
`;

const _isValidDateString = (dateString: string) => {
  if (dateString === undefined) {
    return undefined;
  }

  return DateTime.isValidDateString(dateString)
    ? undefined
    : 'Format must be: YYYY-MM-DD [HH:mm:ss[.SSS]]';
};

const _onFocusSelect = (event) => {
  event.target.select();
};

const zeroPad = (data, pad = 2) => String(data).padStart(pad, '0');

const parseTimeValue = (value, type) => {
  let timeValue = Number(value);

  if (timeValue < 0) timeValue = 0;

  if (type === 'hours') {
    if (timeValue > 23) timeValue = 23;
  } else if (type === 'milliseconds') {
    if (timeValue > 999) timeValue = 999;
  } else if (timeValue > 59) timeValue = 59;

  return timeValue;
};

const fieldUpdate = (value) => {
  const initialDateTime = moment(value).toObject();

  TIME_TYPES.forEach((type) => {
    initialDateTime[type] = zeroPad(initialDateTime[type], type === 'milliseconds' ? 3 : 2);
  });

  const handleChangeDate = (event) => {
    const dateValue = moment(event.target.value, DateTime.Formats.DATE).toObject();
    const { years, months, date, ...initialTime } = initialDateTime;

    return moment({
      years: dateValue.years,
      months: dateValue.months,
      date: dateValue.date,
      ...initialTime,
    }).format(DateTime.Formats.TIMESTAMP);
  };

  const handleChangeSetTime = (event) => {
    const timeType = event.target.getAttribute('name').replace('time-', '');
    const timeValue = parseTimeValue(event.target.value);

    return moment({
      ...initialDateTime,
      [timeType]: timeValue,
    }).format(DateTime.Formats.TIMESTAMP);
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
    handleChangeDate,
    handleChangeSetTime,
    handleClickTimeNow,
    handleTimeToggle,
  };
};

const AbsoluteTimeRangeSelector = ({ disabled, originalTimeRange }: Props) => {
  const fromHourIcon = useRef('hourglass-half');
  const toHourIcon = useRef('hourglass-half');

  return (
    <AbsoluteWrapper>
      <RangeWrapper>
        <Field name="tempTimeRange.from" validate={_isValidDateString}>
          {({ field: { value, onChange, name }, meta: { error } }) => {
            const _onChange = (newValue) => onChange({ target: { name, value: newValue } });
            const fromDateTime = value || originalTimeRange.from;
            const {
              initialDateTime,
              handleChangeDate,
              handleChangeSetTime,
              handleClickTimeNow,
              handleTimeToggle,
            } = fieldUpdate(fromDateTime);

            const _onChangeDate = (event) => _onChange(handleChangeDate(event));

            const _onChangeSetTime = (event) => {
              fromHourIcon.current = 'hourglass-half';

              _onChange(handleChangeSetTime(event));
            };

            const _onClickTimeNow = () => {
              fromHourIcon.current = 'hourglass-half';

              _onChange(handleClickTimeNow());
            };

            const _onClickHourToggle = () => {
              const endOfDay = fromHourIcon.current === 'hourglass-start';

              if (endOfDay) {
                fromHourIcon.current = 'hourglass-end';
              } else {
                fromHourIcon.current = 'hourglass-start';
              }

              _onChange(handleTimeToggle(endOfDay));
            };

            return (
              <>
                <DateInputWithPicker disabled={disabled}
                                     onChange={_onChangeDate}
                                     value={fromDateTime}
                                     name={name}
                                     title="Search start date"
                                     error={error} />

                <div>
                  <SetTimeOption>
                    <FormGroup>
                      <InputGroup>
                        <StyledInputAddon>
                          <StyledButton bsStyle="link"
                                        bsSize="small"
                                        onClick={_onClickHourToggle}>
                            <Icon name={fromHourIcon.current} />
                          </StyledButton>
                        </StyledInputAddon>
                        <StyledFormControl type="text"
                                           id="absolute-from-time-hours"
                                           name="time-hours"
                                           value={initialDateTime.hours}
                                           onChange={_onChangeSetTime}
                                           onFocus={_onFocusSelect}
                                           size={2}
                                           bsSize="sm" />
                        <StyledInputAddon>:</StyledInputAddon>
                        <StyledFormControl type="text"
                                           id="absolute-from-time-minutes"
                                           name="time-minutes"
                                           value={initialDateTime.minutes}
                                           onChange={_onChangeSetTime}
                                           onFocus={_onFocusSelect}
                                           size={2}
                                           bsSize="sm" />
                        <StyledInputAddon>:</StyledInputAddon>
                        <StyledFormControl type="text"
                                           id="absolute-from-time-seconds"
                                           name="time-seconds"
                                           value={initialDateTime.seconds}
                                           onChange={_onChangeSetTime}
                                           onFocus={_onFocusSelect}
                                           size={2}
                                           bsSize="sm" />
                        <StyledInputAddon>.</StyledInputAddon>
                        <StyledFormControl type="text"
                                           id="absolute-from-time-milliseconds"
                                           name="time-milliseconds"
                                           value={initialDateTime.milliseconds}
                                           onChange={_onChangeSetTime}
                                           onFocus={_onFocusSelect}
                                           size={3}
                                           bsSize="sm" />
                        <StyledInputAddon>
                          <StyledButton bsStyle="link"
                                        bsSize="small"
                                        onClick={_onClickTimeNow}>
                            <Icon name="magic" />
                          </StyledButton>
                        </StyledInputAddon>
                      </InputGroup>
                    </FormGroup>
                  </SetTimeOption>
                </div>
              </>
            );
          }}
        </Field>
      </RangeWrapper>

      <IconWrap>
        <Icon name="arrow-right" />
      </IconWrap>

      <RangeWrapper>
        <Field name="tempTimeRange.to" validate={_isValidDateString}>
          {({ field: { value, onChange, onBlur, name }, meta: { error } }) => {
            const _onChange = (newValue) => onChange({ target: { name, value: newValue } });
            const fromDateTime = value || originalTimeRange.from;
            const {
              initialDateTime,
              handleChangeDate,
              handleChangeSetTime,
              handleClickTimeNow,
              handleTimeToggle,
            } = fieldUpdate(fromDateTime);

            const _onChangeDate = (event) => _onChange(handleChangeDate(event));

            const _onChangeSetTime = (event) => {
              toHourIcon.current = 'hourglass-half';

              _onChange(handleChangeSetTime(event));
            };

            const _onClickTimeNow = () => {
              toHourIcon.current = 'hourglass-half';

              _onChange(handleClickTimeNow());
            };

            const _onClickHourToggle = () => {
              const endOfDay = toHourIcon.current === 'hourglass-start';

              if (endOfDay) {
                toHourIcon.current = 'hourglass-end';
              } else {
                toHourIcon.current = 'hourglass-start';
              }

              _onChange(handleTimeToggle(endOfDay));
            };

            return (
              <>
                <DateInputWithPicker disabled={disabled}
                                     onChange={_onChangeDate}
                                     onBlur={onBlur}
                                     value={fromDateTime}
                                     name={name}
                                     title="Search end date"
                                     error={error} />

                <div>
                  <SetTimeOption>
                    <FormGroup>
                      <InputGroup>
                        <StyledInputAddon>
                          <StyledButton bsStyle="link"
                                        bsSize="small"
                                        onClick={_onClickHourToggle}>
                            <Icon name={toHourIcon.current} />
                          </StyledButton>
                        </StyledInputAddon>
                        <StyledFormControl type="text"
                                           id="absolute-to-time-hours"
                                           value={initialDateTime.hours}
                                           onChange={_onChangeSetTime}
                                           onFocus={_onFocusSelect}
                                           size={2}
                                           bsSize="sm" />
                        <StyledInputAddon>:</StyledInputAddon>
                        <StyledFormControl type="text"
                                           id="absolute-to-time-minutes"
                                           value={initialDateTime.minutes}
                                           onChange={_onChangeSetTime}
                                           onFocus={_onFocusSelect}
                                           size={2}
                                           bsSize="sm" />
                        <StyledInputAddon>:</StyledInputAddon>
                        <StyledFormControl type="text"
                                           id="absolute-to-time-seconds"
                                           value={initialDateTime.seconds}
                                           onChange={_onChangeSetTime}
                                           onFocus={_onFocusSelect}
                                           size={2}
                                           bsSize="sm" />
                        <StyledInputAddon>.</StyledInputAddon>
                        <StyledFormControl type="text"
                                           id="absolute-to-time-milliseconds"
                                           value={initialDateTime.milliseconds}
                                           onChange={_onChangeSetTime}
                                           onFocus={_onFocusSelect}
                                           size={3}
                                           bsSize="sm" />
                        <StyledInputAddon>
                          <StyledButton bsStyle="link"
                                        bsSize="small"
                                        onClick={_onClickTimeNow}>
                            <Icon name="magic" />
                          </StyledButton>
                        </StyledInputAddon>
                      </InputGroup>
                    </FormGroup>
                  </SetTimeOption>
                </div>
              </>
            );
          }}
        </Field>
      </RangeWrapper>
    </AbsoluteWrapper>
  );
};

AbsoluteTimeRangeSelector.propTypes = {
  disabled: PropTypes.bool,
  originalTimeRange: PropTypes.shape({ from: PropTypes.string, to: PropTypes.string }).isRequired,
};

AbsoluteTimeRangeSelector.defaultProps = {
  disabled: false,
};

export default AbsoluteTimeRangeSelector;
