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
import { TimeRange, AbsoluteTimeRange } from 'views/logic/queries/Query';

type Props = {
  disabled: boolean,
  from: boolean,
  currentTimerange?: AbsoluteTimeRange,
  originalTimeRange: TimeRange,
  limitDuration?: number,
};

const TIME_TYPES = [
  'hours',
  'minutes',
  'seconds',
  'milliseconds',
];

const TIME_ICON_BOD = 'hourglass-start';
const TIME_ICON_MID = 'hourglass-half';
const TIME_ICON_EOD = 'hourglass-end';

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

const _onFocusSelect = (event) => {
  event.target.select();
};

const zeroPad = (data: string | number, pad = 2) => String(data).padStart(pad, '0');

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

const _isValidDateString = (dateString: string) => {
  if (!dateString) {
    return 'Date is required.';
  }

  if (!DateTime.isValidDateString(dateString)) {
    return 'Format must be: YYYY-MM-DD [HH:mm:ss[.SSS]].';
  }

  return undefined;
};

const AbsoluteRangeField = ({ disabled, limitDuration, from, currentTimerange }: Props) => {
  const range = from ? 'from' : 'to';
  const hourIcon = useRef(TIME_ICON_MID);

  return (
    <Field name={`tempTimeRange[${range}]`} validate={_isValidDateString}>
      {({ field: { value, onChange, name }, meta: { error } }) => {
        const _onChange = (newValue) => onChange({ target: { name, value: newValue } });

        const dateTime = error ? currentTimerange[range] : value || currentTimerange[range];
        const {
          initialDateTime,
          handleChangeSetTime,
          handleClickTimeNow,
          handleTimeToggle,
        } = fieldUpdate(dateTime);

        const _onChangeSetTime = (event) => {
          hourIcon.current = TIME_ICON_MID;

          _onChange(handleChangeSetTime(event));
        };

        const _onClickTimeNow = () => {
          hourIcon.current = TIME_ICON_MID;

          _onChange(handleClickTimeNow());
        };

        const _onClickHourToggle = () => {
          const endOfDay = hourIcon.current === TIME_ICON_BOD;

          if (endOfDay) {
            hourIcon.current = TIME_ICON_EOD;
          } else {
            hourIcon.current = TIME_ICON_BOD;
          }

          _onChange(handleTimeToggle(endOfDay));
        };

        const _onChangeDate = (newDate) => {
          _onChange(newDate);
        };

        let fromDate = moment(currentTimerange.from).toDate();

        if (from) {
          fromDate = limitDuration ? moment().seconds(-limitDuration).toDate() : undefined;
        }

        return (
          <>
            <DateInputWithPicker disabled={disabled}
                                 onChange={_onChangeDate}
                                 value={value || currentTimerange[range]}
                                 initialDateTimeObject={initialDateTime}
                                 name={name}
                                 title="Search end date"
                                 error={error}
                                 fromDate={fromDate} />
            <div>
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
                    <StyledFormControl type="text"
                                       id={`${range}-time-hours`}
                                       title={`${range} hour`}
                                       value={initialDateTime.hours}
                                       onChange={_onChangeSetTime}
                                       onFocus={_onFocusSelect}
                                       size={2}
                                       bsSize="sm" />
                    <StyledInputAddon>:</StyledInputAddon>
                    <StyledFormControl type="text"
                                       id={`${range}-time-minutes`}
                                       title={`${range} minutes`}
                                       value={initialDateTime.minutes}
                                       onChange={_onChangeSetTime}
                                       onFocus={_onFocusSelect}
                                       size={2}
                                       bsSize="sm" />
                    <StyledInputAddon>:</StyledInputAddon>
                    <StyledFormControl type="text"
                                       id={`${range}-time-seconds`}
                                       title={`${range} seconds`}
                                       value={initialDateTime.seconds}
                                       onChange={_onChangeSetTime}
                                       onFocus={_onFocusSelect}
                                       size={2}
                                       bsSize="sm" />
                    <StyledInputAddon>.</StyledInputAddon>
                    <StyledFormControl type="text"
                                       id={`${range}-time-milliseconds`}
                                       title={`${range} milliseconds`}
                                       value={initialDateTime.milliseconds}
                                       onChange={_onChangeSetTime}
                                       onFocus={_onFocusSelect}
                                       size={3}
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
            </div>
          </>
        );
      }}
    </Field>
  );
};

AbsoluteRangeField.propTypes = {
  from: PropTypes.bool.isRequired,
  currentTimerange: PropTypes.shape({
    from: PropTypes.string,
    to: PropTypes.string,
  }),
  originalTimeRange: PropTypes.shape({
    from: PropTypes.string,
    to: PropTypes.string,
  }).isRequired,
  disabled: PropTypes.bool,
  limitDuration: PropTypes.number,
};

AbsoluteRangeField.defaultProps = {
  disabled: false,
  limitDuration: 0,
  currentTimerange: undefined,
};

export default AbsoluteRangeField;
