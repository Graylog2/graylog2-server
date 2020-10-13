// @flow strict
import * as React from 'react';
import { useCallback, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import { Field, useField } from 'formik';
import styled, { css, type StyledComponent } from 'styled-components';

import Input from 'components/bootstrap/Input';
import type { SearchesConfig } from 'components/search/SearchConfig';
import { Icon, Select } from 'components/common';

type Props = {
  disabled: boolean,
  config: SearchesConfig,
};

const rangeValues = [
  {
    value: 'seconds',
    label: 'Seconds',
  }, {
    value: 'minutes',
    label: 'Minutes',
  }, {
    value: 'hours',
    label: 'Hours',
  }, {
    value: 'days',
    label: 'Days',
  }, {
    value: 'weeks',
    label: 'Weeks',
  }, {
    value: 'months',
    label: 'Months',
  },
];

const RelativeWrapper: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-around;
`;

const RangeWrapper: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  flex: 4;
  align-items: center;
  display: grid;
  grid-template-columns: max-content repeat(5, 1fr) max-content;
  grid-template-rows: repeat(2, 1fr);
  grid-column-gap: 0px;
  grid-row-gap: 0px;
  
`;

const InputWrap: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  grid-area: 2 / 1 / 3 / 3;
  
  .form-group {
    margin: 0;
  }
`;

const StyledSelect: StyledComponent<{}, void, typeof Select> = styled(Select)`
  grid-area: 2 / 3 / 3 / 7;
  margin: 0 12px;
`;

const StyledIcon: StyledComponent<{}, void, typeof Icon> = styled(Icon)`
  flex: 0.75;
`;

const RangeTitle = styled.h3`
  grid-area: 1 / 1 / 2 / 2;
`;

const Ago: StyledComponent<{}, void, HTMLSpanElement> = styled.span(({ theme }) => css`
  grid-area: 2 / 7 / 3 / 8;
  font-size: ${theme.fonts.size.large};

  ::after {
    content: 'ago';
  }
`);

const RangeCheck: StyledComponent<{}, void, HTMLLabelElement> = styled.label(({ theme }) => css`
  font-size: ${theme.fonts.size.small};
  grid-area: 1 / 2 / 2 / 7;
  margin-left: 15px;
  font-weight: normal;
  align-self: self-end;
  
  input {
    margin-right: 6px;
  }
`);

export default function RelativeTimeRangeSelector({ config, disabled }: Props) {
  // const timeRangeLimit = moment.duration(config.query_time_range_limit);
  // let options;
  //
  // if (availableOptions) {
  //   let all = null;
  //
  //   options = Object.keys(availableOptions).map((key) => {
  //     const seconds = moment.duration(key).asSeconds();
  //
  //     if (timeRangeLimit.seconds() > 0 && (seconds > timeRangeLimit.asSeconds() || seconds === 0)) {
  //       return null;
  //     }
  //
  //     const option = (<option key={`relative-option-${key}`} value={seconds}>{availableOptions[key]}</option>);
  //
  //     // The "search in all messages" option should be the last one.
  //     if (key === 'PT0S') {
  //       all = option;
  //
  //       return null;
  //     }
  //
  //     return option;
  //   });
  //
  //   if (all) {
  //     options.push(all);
  //   }
  // } else {
  //   options = (<option value="300">Loading...</option>);
  // }

  const [nextRangeProps, , nextRangeHelpers] = useField('tempTimeRange');

  const [fromTimeValue, setFromTimeValue] = useState();
  const [fromTimeRange, setFromTimeRange] = useState(rangeValues[0].value);

  const getRangeSeconds = useCallback((value = fromTimeValue, range = fromTimeRange) => {
    return moment.duration(value, range).asSeconds();
  }, [fromTimeValue, fromTimeRange]);

  const setRange = (range) => {
    const ms = moment.duration(range, fromTimeRange).asMilliseconds();
    const difference = moment().subtract(ms);

    const nextRange = rangeValues.map(({ value }) => {
      const diff = Number.parseFloat((moment().diff(difference, value, true)).toFixed(2));

      if (diff && diff - Math.floor(diff) === 0) {
        return { diff, value };
      }

      return null;
    }).filter(Boolean).pop();

    setFromTimeValue(nextRange.diff);
    setFromTimeRange(nextRange.value);
  };

  useEffect(() => {
    if (nextRangeProps.value?.range && nextRangeProps.value.range !== getRangeSeconds()) {
      setRange(nextRangeProps.value.range);
    }
  }, [nextRangeProps.value]);

  return (
    <RelativeWrapper>
      <RangeWrapper>
        <Field name="tempTimeRange.from">
          {({ field: { name, onChange } }) => {
            const _onChange = (event) => {
              let fromMs;

              if (typeof event === 'string') {
                setFromTimeRange(event);

                fromMs = getRangeSeconds(undefined, event);
              } else {
                setFromTimeValue(event.target.value);
                fromMs = getRangeSeconds(event.target.value);
              }

              nextRangeHelpers.setValue({ ...nextRangeProps.value, range: fromMs });
              onChange(fromMs);
            };

            return (
              <>
                <RangeTitle>From:</RangeTitle>
                <RangeCheck htmlFor="relative-all-time">
                  <input type="checkbox" id="relative-all-time" value="0" />All Time
                </RangeCheck>
                <InputWrap>
                  <Input id="relative-timerange-from-value"
                         disabled={disabled}
                         type="number"
                         value={fromTimeValue}
                         title="Set the range value"
                         name={name}
                         onChange={_onChange} />
                </InputWrap>
                <StyledSelect id="relative-timerange-from-length"
                              disabled={disabled}
                              value={fromTimeRange}
                              options={rangeValues}
                              placeholder="Select a range"
                              name={name}
                              onChange={_onChange}
                              clearable={false} />

                <Ago />
              </>
            );
          }}
        </Field>
      </RangeWrapper>

      <StyledIcon name="arrow-right" />

      <RangeWrapper>
        <RangeTitle>Until:</RangeTitle>
        <RangeCheck htmlFor="relative-now">
          <input type="checkbox" id="relative-now" checked disabled />Now
        </RangeCheck>
        <Field name="tempTimeRange.to.number">
          {({ field: { name, onChange } }) => {
            return (
              <InputWrap>
                <Input id="relative-timerange-until-value"
                       disabled
                       type="number"
                       value="0"
                       title="Set the range value"
                       name={name}
                       onChange={onChange} />
              </InputWrap>
            );
          }}
        </Field>

        <Field name="tempTimeRange.to.length">
          {({ field: { name, onChange } }) => {
            return (
              <StyledSelect id="relative-timerange-until-length"
                            disabled
                            value={rangeValues[0].value}
                            options={rangeValues}
                            placeholder="Select a range"
                            name={name}
                            onChange={onChange} />
            );
          }}
        </Field>
        <Ago />
      </RangeWrapper>
    </RelativeWrapper>
  );
}

RelativeTimeRangeSelector.propTypes = {
  config: PropTypes.shape({
    query_time_range_limit: PropTypes.string.isRequired,
  }).isRequired,
  disabled: PropTypes.bool,
};

RelativeTimeRangeSelector.defaultProps = {
  disabled: false,
};
