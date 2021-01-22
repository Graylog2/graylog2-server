import * as React from 'react';
import { Field, useFormikContext } from 'formik';
import styled, { css } from 'styled-components';
import moment from 'moment';

import { RELATIVE_RANGE_TYPES } from 'views/Constants';
import Input from 'components/bootstrap/Input';
import { Select } from 'components/common';

import type { TimeRangeDropDownFormValues } from './TimeRangeDropdown';
import ConfiguredRelativeTimeRangeSelector from './ConfiguredRelativeTimeRangeSelector';

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
  grid-area: 3 / 1 / 3 / 4;
  font-size: ${theme.fonts.size.small};
  font-style: italic;
  padding: 3px;
`);

const ConfiguredWrapper = styled.div`
  grid-area: 3 / 5 / 3 / 7;
  margin: 3px 12px 3px 0;
  justify-self: end;
`;

const getValue = (fieldName, value: number) => RELATIVE_RANGE_TYPES.map(({ type }) => {
  const unsetRange = value === 0 || value === undefined;
  const diff = moment.duration(value, 'seconds').as(type);

  if (diff - Math.floor(diff) === 0) {
    return {
      rangeValue: diff || 0,
      rangeType: unsetRange ? 'seconds' : type,
      unsetRange,
      [fieldName]: value,
    };
  }

  return null;
}).filter(Boolean).pop();

const buildRangeTypes = (limitDuration) => RELATIVE_RANGE_TYPES.map(({ label, type }) => {
  const typeDuration = moment.duration(1, type).asSeconds();

  if (limitDuration === 0 || typeDuration <= limitDuration) {
    return { label, value: type };
  }

  return null;
}).filter(Boolean);

type Props = {
  disabled?: boolean,
  fieldName: 'range' | 'from' | 'to',
  limitDuration: number,
  unsetRangeLabel: string,
  title: string,
  defaultRange: number
  disableUnsetRange?: boolean
}

const RelativeRangeSelect = ({ disabled, fieldName, limitDuration, unsetRangeLabel, defaultRange, title, disableUnsetRange }: Props) => {
  const { initialValues } = useFormikContext<TimeRangeDropDownFormValues>();
  const availableRangeTypes = buildRangeTypes(limitDuration);

  return (
    <Field name={`nextTimeRange.${fieldName}`}>
      {({ field: { value, onChange, name }, meta: { error } }) => {
        const inputValue = getValue(fieldName, value);

        const _onChange = (nextValue) => {
          onChange({ target: { name, value: nextValue } });
        };

        const _onChangeTime = (event) => {
          const newTimeValue = moment.duration(event.target.value || 1, inputValue.rangeType).asSeconds();

          _onChange(newTimeValue);
        };

        const _onChangeType = (type) => {
          const newTimeValue = moment.duration(inputValue.rangeValue, type).asSeconds();

          _onChange(newTimeValue);
        };

        const _onUnsetRange = (event) => {
          const hasInitialRelativeRange = 'type' in initialValues.nextTimeRange && initialValues.nextTimeRange.type === 'relative';
          const _defaultRange = (
            hasInitialRelativeRange
            && fieldName in initialValues.nextTimeRange
            && initialValues.nextTimeRange[fieldName]
          ) ? initialValues.nextTimeRange[fieldName] : defaultRange;

          _onChange(event.target.checked ? 0 : _defaultRange);
        };

        const _onSetPreset = (range) => {
          const newFromValue = getValue(fieldName, range);

          _onChange(newFromValue[fieldName]);
        };

        return (
          <RangeWrapper>
            <RangeTitle>{title}</RangeTitle>
            <RangeCheck htmlFor={`relative-unset-${fieldName}`} className={disableUnsetRange && 'shortened'}>
              <input type="checkbox"
                     id={`relative-unset-${fieldName}`}
                     value="0"
                     className="mousetrap"
                     checked={inputValue.unsetRange}
                     onChange={_onUnsetRange}
                     disabled={disableUnsetRange} />{unsetRangeLabel}
            </RangeCheck>
            <InputWrap>
              <Input id={`relative-timerange-${fieldName}-value`}
                     name={`relative-timerange-${fieldName}-value`}
                     disabled={disabled || inputValue.unsetRange}
                     type="number"
                     min="1"
                     value={inputValue.rangeValue}
                     className="mousetrap"
                     title={`Set the ${fieldName} value`}
                     onChange={_onChangeTime}
                     bsStyle={error ? 'error' : null} />
            </InputWrap>
            <StyledSelect id={`relative-timerange-${fieldName}-length`}
                          name={`relative-timerange-${fieldName}-length`}
                          disabled={disabled || inputValue.unsetRange}
                          value={inputValue.rangeType}
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

            <ConfiguredWrapper>
              <ConfiguredRelativeTimeRangeSelector onChange={_onSetPreset} />
            </ConfiguredWrapper>
          </RangeWrapper>

        );
      }}
    </Field>
  );
};

RelativeRangeSelect.defaultProps = {
  disabled: false,
  disableUnsetRange: false,
};

export default RelativeRangeSelect;
