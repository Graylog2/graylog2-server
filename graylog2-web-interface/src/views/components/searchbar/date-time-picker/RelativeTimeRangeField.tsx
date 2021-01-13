import * as React from 'react';
import { Field } from 'formik';
import styled, { css } from 'styled-components';
import moment from 'moment';
import { RelativeTimeRange, TimeRange } from 'src/views/logic/queries/Query';

import Input from 'components/bootstrap/Input';
import { Select } from 'components/common';
import { RELATIVE_RANGE_TYPES } from 'views/Constants';

const RangeWrapper = styled.div`
  flex: 4;
  align-items: center;
  display: grid;
  grid-template-columns: max-content repeat(5, 1fr) max-content;
  grid-template-rows: repeat(2, 1fr) minmax(1.5em, auto);
  grid-column-gap: 0;
  grid-row-gap: 0;
`;

const InputWrap = styled.div`
  grid-area: 2 / 1 / 3 / 3;
  position: relative;
  
  .form-group {
    margin: 0;
  }
`;

const StyledSelect = styled(Select)`
  grid-area: 2 / 3 / 3 / 7;
  margin: 0 12px;
`;

const RangeTitle = styled.h3`
  grid-area: 1 / 1 / 2 / 2;
`;

const Ago = styled.span(({ theme }) => css`
  grid-area: 2 / 7 / 3 / 8;
  font-size: ${theme.fonts.size.large};

  ::after {
    content: 'ago';
  }
`);

const RangeCheck = styled.label(({ theme }) => css`
  font-size: ${theme.fonts.size.small};
  grid-area: 1 / 2 / 2 / 8;
  margin-left: 15px;
  font-weight: normal;
  align-self: self-end;
  
  &.shortened {
    grid-area: 1 / 2 / 2 / 4;
    text-decoration: line-through;
    cursor: not-allowed;
  }
  
  input {
    margin-right: 6px;
  }
`);

const ErrorMessage = styled.span(({ theme }) => css`
  color: ${theme.colors.variant.dark.danger};
  grid-area: 3 / 1 / 3 / 8;
  font-size: ${theme.fonts.size.tiny};
  font-style: italic;
  padding: 3px;
`);

type Props = {
  availableRangeTypes: Array<{
    label: 'Seconds' | 'Minutes' | 'Hours' | 'Days' | 'Weeks';
    value: 'seconds' | 'minutes' | 'hours' | 'days' | 'weeks';
  }>,
  disableUnsetRange?: boolean,
  unsetRangeLabel: string,
  defaultRange: RelativeTimeRange['range'],
  disabled: boolean,
  name: string,
  originalTimeRange: TimeRange,
  title: string
}

const RelativeTimeRangeField = ({
  availableRangeTypes,
  disableUnsetRange,
  unsetRangeLabel,
  defaultRange,
  disabled,
  name,
  originalTimeRange,
  title,
}: Props) => {
  return (
    <Field name={name}>
      {({ field: { value, onChange }, meta: { error } }) => {
        const fromValue = RELATIVE_RANGE_TYPES.map(({ type }) => {
          const isNotSpecified = value === 0 || value === undefined;
          const diff = moment.duration(value, 'seconds').as(type);

          if (diff - Math.floor(diff) === 0) {
            return {
              ...originalTimeRange,
              rangeValue: diff || 0,
              rangeType: isNotSpecified ? 'seconds' : type,
              rangeAllTime: isNotSpecified,
              range: value,
            };
          }

          return null;
        }).filter(Boolean).pop();

        const _onChange = (nextValue) => onChange({ target: { name, value: nextValue } });

        const _onChangeTime = (event) => {
          const newTimeValue = moment.duration(event.target.value || 1, fromValue.rangeType).asSeconds();

          _onChange(newTimeValue);
        };

        const _onChangeType = (type) => {
          const newTimeValue = moment.duration(fromValue.rangeValue, type).asSeconds();

          _onChange(newTimeValue);
        };

        const _onCheckAllTime = (event) => {
          _onChange(event.target.checked ? 0 : defaultRange);
        };

        return (
          <RangeWrapper>
            <RangeTitle>{title}</RangeTitle>
            <RangeCheck htmlFor={`${name}-not-a-range`} className={disableUnsetRange && 'shortened'}>
              <input type="checkbox"
                     id={`${name}-not-a-range`}
                     value="0"
                     className="mousetrap"
                     checked={fromValue.rangeAllTime}
                     onChange={_onCheckAllTime}
                     disabled={disableUnsetRange} />{unsetRangeLabel}
            </RangeCheck>
            <InputWrap>
              <Input id={`${name}-value`}
                     name={`${name}-from-value`}
                     disabled={disabled || fromValue.rangeAllTime}
                     type="number"
                     min="1"
                     value={fromValue.rangeValue}
                     className="mousetrap"
                     title="Set the range value"
                     onChange={_onChangeTime}
                     bsStyle={error ? 'error' : null} />
            </InputWrap>
            <StyledSelect id={`${name}-length`}
                          name={`${name}-length`}
                          disabled={disabled || fromValue.rangeAllTime}
                          value={fromValue.rangeType}
                          options={availableRangeTypes}
                          inputProps={{ className: 'mousetrap' }}
                          placeholder="Select a range length"
                          onChange={_onChangeType}
                          clearable={false} />

            <Ago />
            {error && (
              <ErrorMessage>
                {error}
              </ErrorMessage>
            )}
          </RangeWrapper>
        );
      }}
    </Field>
  );
};

RelativeTimeRangeField.defaultProps = {
  disableUnsetRange: false,
};

export default RelativeTimeRangeField;
