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
import { useState } from 'react';
import PropTypes from 'prop-types';
import { Field } from 'formik';
import styled, { css } from 'styled-components';
import type { StyledComponent } from 'styled-components';
import moment from 'moment';

import DateTime from 'logic/datetimes/DateTime';
import { Icon, Select } from 'components/common';
import { Input } from 'components/bootstrap';
import {
  FormGroup,
  InputGroup,
  FormControl,
} from 'components/graylog';
import DateInputWithPicker from 'views/components/searchbar/DateInputWithPicker';
import type { ThemeInterface } from 'theme';

type Props = {
  disabled: boolean,
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

const StyledLabel: StyledComponent<{}, void, HTMLLabelElement> = styled.label`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 15px;
  font-weight: normal;
  margin: 0 0 6px;
  
  .form-group { margin: 0; }
  
  > input[type='radio'] {
    margin: 0 3px 0 0;
  }
`;

const LabelText: StyledComponent<{}, ThemeInterface, HTMLSpanElement> = styled.span(({ theme }) => css`
  flex: 1;
  font-size: ${theme.fonts.size.small};
`);

const SetTimeOption: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex: 1;

  b { padding: 0 3px; }
`;

const FlexSelect: StyledComponent<{}, void, typeof Select> = styled(Select)`
  flex: .75;
`;

const StyledInputAddon: StyledComponent<{}, ThemeInterface, typeof InputGroup.Addon> = styled(InputGroup.Addon)(({ theme }) => css`
  padding: 0 9px;
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

const _isValidDateString = (dateString: string) => {
  if (dateString === undefined) {
    return undefined;
  }

  return DateTime.isValidDateString(dateString)
    ? undefined
    : 'Format must be: YYYY-MM-DD [HH:mm:ss[.SSS]]';
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

const _generateSetHours = () => {
  let minHours = 0;
  const maxHours = 23;

  const setHours = [];

  while (minHours <= maxHours) {
    setHours.push({
      value: zeroPad(String(minHours)),
      label: zeroPad(String(minHours)),
    });

    minHours += 1;
  }

  return setHours;
};

const AbsoluteTimeRangeSelector = ({ disabled, originalTimeRange }: Props) => {
  const [fromTimeOption, setFromTimeOption] = useState('set-time');
  const [toTimeOption, setToTimeOption] = useState('set-time');

  const _handleChangeFromOption = (event) => {
    setFromTimeOption(event.target.value);
  };

  const _handleChangeToOption = (event) => {
    setToTimeOption(event.target.value);
  };

  return (
    <AbsoluteWrapper>
      <RangeWrapper>
        <Field name="tempTimeRange.from" validate={_isValidDateString}>
          {({ field: { value, onChange, name }, meta: { error } }) => {
            const newFromValue = {};

            TIME_TYPES.forEach((type) => {
              newFromValue[type] = zeroPad(moment(value || originalTimeRange.from)[type](), type === 'milliseconds' ? 3 : 2);
            });

            const _onFocusSetTime = (event) => {
              event.target.select();
            };

            const _onChange = (newValue) => onChange({ target: { name, value: newValue } });

            const _onChangeDate = (event) => {
              const dateValue = event.target.value.split(' ');
              let newDate;

              const currentTime = {};

              Object.keys(newFromValue).forEach((type) => {
                currentTime[type] = Number(newFromValue[type]);
              });

              if (dateValue[1]) {
                newDate = moment(dateValue.join(' '))
                  .format(DateTime.Formats.TIMESTAMP);
              } else {
                newDate = moment(dateValue[0])
                  .hours(currentTime.hours)
                  .minutes(currentTime.minutes)
                  .seconds(currentTime.seconds)
                  .milliseconds(currentTime.milliseconds)
                  .format(DateTime.Formats.TIMESTAMP);
              }

              _onChange(newDate);
            };

            const _onChangeSetTime = (event) => {
              const timeType = event.target.getAttribute('id').replace('absolute-from-time-', '');
              const timeValue = parseTimeValue(event.target.value);

              const currentTime = moment(value);
              const updatedTime = currentTime[timeType](timeValue);
              const formattedTime = updatedTime.format(DateTime.Formats.TIMESTAMP);

              _onChange(formattedTime);
            };

            const _onChangeHour = (hours) => {
              const newDate = moment(value)
                .set({ hours, minutes: 0, seconds: 0, milliseconds: 0 })
                .format(DateTime.Formats.TIMESTAMP);

              _onChange(newDate);
            };

            return (
              <>
                <DateInputWithPicker disabled={disabled}
                                     onChange={_onChangeDate}
                                     value={value}
                                     name={name}
                                     title="Search start date"
                                     error={error} />

                <div>
                  <StyledLabel htmlFor="absolute-from-time">
                    <input type="radio"
                           id="absolute-from-time"
                           name="from-time-option"
                           value="set-time"
                           checked={fromTimeOption === 'set-time'}
                           onChange={_handleChangeFromOption} />

                    <SetTimeOption>
                      <FormGroup>
                        <InputGroup>
                          <StyledInputAddon><Icon name="hourglass-start" /></StyledInputAddon>
                          <StyledFormControl type="text"
                                             id="absolute-from-time-hours"
                                             value={newFromValue.hours}
                                             onChange={_onChangeSetTime}
                                             onFocus={_onFocusSetTime}
                                             size={2}
                                             bsSize="sm" />
                          <StyledInputAddon>:</StyledInputAddon>
                          <StyledFormControl type="text"
                                             id="absolute-from-time-minutes"
                                             value={newFromValue.minutes}
                                             onChange={_onChangeSetTime}
                                             onFocus={_onFocusSetTime}
                                             size={2}
                                             bsSize="sm" />
                          <StyledInputAddon>:</StyledInputAddon>
                          <StyledFormControl type="text"
                                             id="absolute-from-time-seconds"
                                             value={newFromValue.seconds}
                                             onChange={_onChangeSetTime}
                                             onFocus={_onFocusSetTime}
                                             size={2}
                                             bsSize="sm" />
                          <StyledInputAddon>.</StyledInputAddon>
                          <StyledFormControl type="text"
                                             id="absolute-from-time-milliseconds"
                                             value={newFromValue.milliseconds}
                                             onChange={_onChangeSetTime}
                                             onFocus={_onFocusSetTime}
                                             size={3}
                                             bsSize="sm" />
                          <StyledInputAddon><Icon name="magic" /></StyledInputAddon>
                        </InputGroup>
                      </FormGroup>
                    </SetTimeOption>
                  </StyledLabel>

                  <StyledLabel htmlFor="absolute-from-hour">
                    <input type="radio"
                           id="absolute-from-hour"
                           name="from-time-option"
                           value="set-hour"
                           checked={fromTimeOption === 'set-hour'}
                           onChange={_handleChangeFromOption} />
                    <LabelText>Beginning of Hour</LabelText>
                    <SetTimeOption>
                      <FlexSelect id="absolute-from-set-hour"
                                  onChange={_onChangeHour}
                                  options={_generateSetHours()}
                                  clearable={false}
                                  value={String(newFromValue.hours)}
                                  size="small" />
                    </SetTimeOption>
                  </StyledLabel>

                  <StyledLabel htmlFor="absolute-from-day">
                    <input type="radio"
                           id="absolute-from-day"
                           name="from-time-option"
                           value="set-day"
                           checked={fromTimeOption === 'set-day'}
                           onChange={_handleChangeFromOption} />
                    <LabelText>Beginning of Day</LabelText>
                    <SetTimeOption>
                      <code>00:00:00.000</code>
                    </SetTimeOption>
                  </StyledLabel>
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
            const newToValue = {};

            TIME_TYPES.forEach((type) => {
              newToValue[type] = zeroPad(moment(value || originalTimeRange.to)[type](), type === 'milliseconds' ? 3 : 2);
            });

            const _onFocusSetTime = (event) => {
              event.target.select();
            };

            const _onChange = (newValue) => onChange({ target: { name, value: newValue } });

            const _onChangeDate = (event) => {
              const dateValue = event.target.value.split(' ');
              let newDate;

              const currentTime = {};

              Object.keys(newToValue).forEach((type) => {
                currentTime[type] = Number(newToValue[type]);
              });

              if (dateValue[1]) {
                newDate = moment(dateValue.join(' '))
                  .format(DateTime.Formats.TIMESTAMP);
              } else {
                newDate = moment(dateValue[0])
                  .hours(currentTime.hours)
                  .minutes(currentTime.minutes)
                  .seconds(currentTime.seconds)
                  .milliseconds(currentTime.milliseconds)
                  .format(DateTime.Formats.TIMESTAMP);
              }

              _onChange(newDate);
            };

            const _onChangeSetTime = (event) => {
              const timeType = event.target.getAttribute('id').replace('absolute-to-time-', '');
              const timeValue = parseTimeValue(event.target.value);

              const currentTime = moment(value);
              const updatedTime = currentTime[timeType](timeValue);
              const formattedTime = updatedTime.format(DateTime.Formats.TIMESTAMP);

              _onChange(formattedTime);
            };

            const _onChangeHour = (hours) => {
              const newDate = moment(value)
                .set({ hours, minutes: 0, seconds: 0, milliseconds: 0 })
                .format(DateTime.Formats.TIMESTAMP);

              _onChange(newDate);
            };

            return (
              <>
                <DateInputWithPicker disabled={disabled}
                                     onChange={_onChangeDate}
                                     onBlur={onBlur}
                                     value={value}
                                     name={name}
                                     title="Search end date"
                                     error={error} />

                <div>
                  <StyledLabel htmlFor="absolute-to-time">
                    <input type="radio"
                           id="absolute-to-time"
                           name="from-to-option"
                           value="set-time"
                           checked={toTimeOption === 'set-time'}
                           onChange={_handleChangeToOption} />

                    <SetTimeOption>
                      <FormGroup>
                        <InputGroup>
                          <StyledInputAddon><Icon name="hourglass-start" /></StyledInputAddon>
                          <StyledFormControl type="text"
                                             id="absolute-to-time-hours"
                                             value={newToValue.hours}
                                             onChange={_onChangeSetTime}
                                             onFocus={_onFocusSetTime}
                                             size={2}
                                             bsSize="sm" />
                          <StyledInputAddon>:</StyledInputAddon>
                          <StyledFormControl type="text"
                                             id="absolute-to-time-minutes"
                                             value={newToValue.minutes}
                                             onChange={_onChangeSetTime}
                                             onFocus={_onFocusSetTime}
                                             size={2}
                                             bsSize="sm" />
                          <StyledInputAddon>:</StyledInputAddon>
                          <StyledFormControl type="text"
                                             id="absolute-to-time-seconds"
                                             value={newToValue.seconds}
                                             onChange={_onChangeSetTime}
                                             onFocus={_onFocusSetTime}
                                             size={2}
                                             bsSize="sm" />
                          <StyledInputAddon>.</StyledInputAddon>
                          <StyledFormControl type="text"
                                             id="absolute-to-time-milliseconds"
                                             value={newToValue.milliseconds}
                                             onChange={_onChangeSetTime}
                                             onFocus={_onFocusSetTime}
                                             size={3}
                                             bsSize="sm" />
                          <StyledInputAddon><Icon name="magic" /></StyledInputAddon>
                        </InputGroup>
                      </FormGroup>
                    </SetTimeOption>
                  </StyledLabel>

                  <StyledLabel htmlFor="absolute-to-hour">
                    <input type="radio"
                           id="absolute-to-hour"
                           name="from-to-option"
                           value="set-hour"
                           checked={toTimeOption === 'set-hour'}
                           onChange={_handleChangeToOption} />

                    <LabelText>End of Hour</LabelText>
                    <SetTimeOption>
                      <FlexSelect id="absolute-to-set-hour"
                                  onChange={_onChangeHour}
                                  options={_generateSetHours()}
                                  clearable={false}
                                  value={newToValue.hours}
                                  size="small" />
                    </SetTimeOption>
                  </StyledLabel>

                  <StyledLabel htmlFor="absolute-to-day">
                    <input type="radio"
                           id="absolute-to-day"
                           name="from-to-option"
                           value="set-day"
                           checked={toTimeOption === 'set-day'}
                           onChange={_handleChangeToOption} />
                    <LabelText>End of Day</LabelText>
                    <SetTimeOption>
                      <code>23:59:59.999</code>
                    </SetTimeOption>
                  </StyledLabel>
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
