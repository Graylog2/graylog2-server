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
import PropTypes from 'prop-types';
import { Field } from 'formik';
import styled, { css } from 'styled-components';
import type { StyledComponent } from 'styled-components';
import moment from 'moment';

import DateTime from 'logic/datetimes/DateTime';
import { Icon } from 'components/common';
import { Input } from 'components/bootstrap';
// import type { ThemeInterface } from 'theme';
import DateInputWithPicker from 'views/components/searchbar/DateInputWithPicker';

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

const FromTimeWrapper: StyledComponent<{}, void, HTMLDivElement> = styled.div``;
const ToTimeWrapper: StyledComponent<{}, void, HTMLDivElement> = styled.div``;

const Label: StyledComponent<{}, void, HTMLLabelElement> = styled.label`
  display: flex;
  justify-content: space-between;
  
  .form-group { margin: 0; }
`;

const SetTimeOption: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  display: flex;
  align-items: center;
  
  b { padding: 0 3px; }
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

const AbsoluteTimeRangeSelector = ({ disabled, originalTimeRange }: Props) => {
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
              const value = event.target.value.split(' ');
              let newDate;

              const currentTime = {};

              Object.keys(newFromValue).forEach((type) => {
                currentTime[type] = Number(newFromValue[type]);
              });

              if (value[1]) {
                newDate = moment(value.join(' '))
                  .format(DateTime.Formats.TIMESTAMP);
              } else {
                newDate = moment(value[0])
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

            return (
              <>
                <DateInputWithPicker disabled={disabled}
                                     onChange={_onChangeDate}
                                     value={value}
                                     name={name}
                                     title="Search start date"
                                     error={error} />

                <FromTimeWrapper>
                  <Label>
                    <Input type="radio" id="absolute-from-time" />
                    <SetTimeOption>
                      <Input type="text"
                             id="absolute-from-time-hours"
                             value={newFromValue.hours}
                             onChange={_onChangeSetTime}
                             onFocus={_onFocusSetTime}
                             size={2} />
                      <b>:</b>
                      <Input type="text"
                             id="absolute-from-time-minutes"
                             value={newFromValue.minutes}
                             onChange={_onChangeSetTime}
                             onFocus={_onFocusSetTime}
                             size={2} />
                      <b>:</b>
                      <Input type="text"
                             id="absolute-from-time-seconds"
                             value={newFromValue.seconds}
                             onChange={_onChangeSetTime}
                             onFocus={_onFocusSetTime}
                             size={2} />
                      <b>.</b>
                      <Input type="text"
                             id="absolute-from-time-milliseconds"
                             value={newFromValue.milliseconds}
                             onChange={_onChangeSetTime}
                             onFocus={_onFocusSetTime}
                             size={3} />
                    </SetTimeOption>
                  </Label>
                </FromTimeWrapper>
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

            const _onChangeSetTime = (event) => {
              const timeType = event.target.getAttribute('id').replace('absolute-to-time-', '');
              const timeValue = parseTimeValue(event.target.value, timeType);

              const currentTime = moment(value);
              const updatedTime = currentTime[timeType](timeValue);
              const formattedTime = updatedTime.format(DateTime.Formats.TIMESTAMP);

              onChange({ target: { name, value: formattedTime } });
            };

            return (
              <>
                <DateInputWithPicker disabled={disabled}
                                     onChange={onChange}
                                     onBlur={onBlur}
                                     value={value}
                                     name={name}
                                     title="Search end date"
                                     error={error} />

                <ToTimeWrapper>
                  <Label>
                    <Input type="radio" id="absolute-to-time" />
                    <SetTimeOption>
                      <Input type="text"
                             id="absolute-to-time-hours"
                             value={newToValue.hours}
                             onChange={_onChangeSetTime}
                             onFocus={_onFocusSetTime}
                             size={2} /><b>:</b>
                      <Input type="text"
                             id="absolute-to-time-minutes"
                             value={newToValue.minutes}
                             onChange={_onChangeSetTime}
                             onFocus={_onFocusSetTime}
                             size={2} /><b>:</b>
                      <Input type="text"
                             id="absolute-to-time-seconds"
                             value={newToValue.seconds}
                             onChange={_onChangeSetTime}
                             onFocus={_onFocusSetTime}
                             size={2} /><b>.</b>
                      <Input type="text"
                             id="absolute-to-time-milliseconds"
                             value={newToValue.milliseconds}
                             onChange={_onChangeSetTime}
                             onFocus={_onFocusSetTime}
                             size={3} />
                    </SetTimeOption>
                  </Label>
                </ToTimeWrapper>
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
  originalTimeRange: PropTypes.shape({ from: PropTypes.string }).isRequired,
};

AbsoluteTimeRangeSelector.defaultProps = {
  disabled: false,
};

export default AbsoluteTimeRangeSelector;
