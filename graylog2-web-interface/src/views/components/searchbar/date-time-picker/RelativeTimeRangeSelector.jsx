// @flow strict
import * as React from 'react';
import { useEffect, useReducer } from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import { useField } from 'formik';
import styled, { css, type StyledComponent } from 'styled-components';

import { type ThemeInterface } from 'theme';
import Input from 'components/bootstrap/Input';
import type { SearchesConfig } from 'components/search/SearchConfig';
import { Icon, Select } from 'components/common';

type Props = {
  disabled: boolean,
  config: SearchesConfig,
  originalTimeRange: {
    range: string | number,
  },
};

const RANGE_VALUES = [
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

const Ago: StyledComponent<{}, ThemeInterface, HTMLSpanElement> = styled.span(({ theme }) => css`
  grid-area: 2 / 7 / 3 / 8;
  font-size: ${theme.fonts.size.large};

  ::after {
    content: 'ago';
  }
`);

const RangeCheck: StyledComponent<{}, ThemeInterface, HTMLLabelElement> = styled.label(({ theme }) => css`
  font-size: ${theme.fonts.size.small};
  grid-area: 1 / 2 / 2 / 7;
  margin-left: 15px;
  font-weight: normal;
  align-self: self-end;
  
  input {
    margin-right: 6px;
  }
`);

const initialRangeType = ({ range, ...restRange }) => {
  if (range === 0) {
    return {
      rangeValue: 1,
      rangeType: 'seconds',
      range,
      ...restRange,
    };
  }

  return RANGE_VALUES.map(({ value }) => {
    const diff = moment.duration(range, 'seconds').as(value);

    if (diff - Math.floor(diff) === 0) {
      return {
        rangeValue: diff || '1',
        rangeType: value || 'seconds',
        range,
        ...restRange,
      };
    }

    return null;
  }).filter(Boolean).pop();
};

function reducer(state, action) {
  switch (action.type) {
    case 'rangeValue':
      return {
        ...state,
        rangeValue: action.value,
        range: moment.duration(action.value, state.rangeType).asSeconds(),
      };
    case 'rangeType':
      return {
        ...state,
        rangeType: action.value,
        range: moment.duration(state.rangeValue, action.value).asSeconds(),
      };
    case 'rangeAllTime':
      return {
        ...state,
        rangeAllTime: action.value,
        range: action.value ? 0 : state.rangeValue,
      };
    default:
      throw new Error();
  }
}

const buildRangeTypes = (config) => RANGE_VALUES.map(({ label, value }) => {
  const typeDuration = moment.duration(1, value).asSeconds();
  const limitDuration = moment.duration(config.query_time_range_limit).asSeconds();

  if (limitDuration === 0 || typeDuration <= limitDuration) {
    return { label, value };
  }

  return null;
}).filter(Boolean);

export default function RelativeTimeRangeSelector({ config, disabled, originalTimeRange }: Props) {
  const [nextRangeProps, , nextRangeHelpers] = useField('tempTimeRange');

  const [state, dispatch] = useReducer(reducer, {
    range: originalTimeRange.range,
    rangeAllTime: originalTimeRange.range === 0,
  }, initialRangeType);

  const availableRangeTypes = buildRangeTypes(config);

  useEffect(() => {
    const typeDuration = moment.duration(state.rangeValue, state.rangeType).asSeconds();
    const limitDuration = moment.duration(config.query_time_range_limit).asSeconds();

    if (typeDuration <= limitDuration) {
      nextRangeHelpers.setValue({ ...nextRangeProps.value, ...state });
    } else {
      console.log('Nuh uh uh - you went over the limit!');
    }
  }, [state]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <RelativeWrapper>
      <RangeWrapper>
        <RangeTitle>From:</RangeTitle>
        <RangeCheck htmlFor="relative-all-time">
          <input type="checkbox"
                 id="relative-all-time"
                 value="0"
                 checked={state.rangeAllTime}
                 onChange={(event) => dispatch({
                   type: 'rangeAllTime',
                   value: event.target.checked,
                 })} />All Time
        </RangeCheck>
        <InputWrap>
          <Input id="relative-timerange-from-value"
                 disabled={disabled || state.rangeAllTime}
                 type="number"
                 value={state.rangeValue}
                 min="1"
                 title="Set the range value"
                 name="relative-timerange-from-value"
                 onChange={(event) => dispatch({
                   type: 'rangeValue',
                   value: event.target.value,
                 })} />
        </InputWrap>
        <StyledSelect id="relative-timerange-from-length"
                      disabled={disabled || state.rangeAllTime}
                      value={state.rangeType}
                      options={availableRangeTypes}
                      placeholder="Select a range"
                      name="relative-timerange-from-length"
                      onChange={(event) => dispatch({
                        type: 'rangeType',
                        value: event,
                      })}
                      clearable={false} />

        <Ago />
      </RangeWrapper>

      <StyledIcon name="arrow-right" />

      <RangeWrapper>
        <RangeTitle>Until:</RangeTitle>
        <RangeCheck htmlFor="relative-offset">
          <input type="checkbox" id="relative-offset" checked disabled />Now
        </RangeCheck>

        <InputWrap>
          <Input id="relative-timerange-until-value"
                 disabled
                 type="number"
                 value="0"
                 min="1"
                 title="Set the range value"
                 name="relative-timerange-until-value"
                 onChange={() => {}} />
        </InputWrap>

        <StyledSelect id="relative-timerange-until-length"
                      disabled
                      value={RANGE_VALUES[0].value}
                      options={availableRangeTypes}
                      placeholder="Select a range"
                      name="relative-timerange-until-length"
                      onChange={() => {}} />
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
  originalTimeRange: PropTypes.shape({
    range: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  }).isRequired,
};

RelativeTimeRangeSelector.defaultProps = {
  disabled: false,
};
