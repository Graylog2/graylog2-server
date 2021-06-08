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
import { useEffect, useState } from 'react';
import { Field, useFormikContext } from 'formik';
import styled, { css } from 'styled-components';
import moment from 'moment';

import { RELATIVE_RANGE_TYPES } from 'views/Constants';
import { Select } from 'components/common';
import { isTypeRelative } from 'views/typeGuards/timeRange';

import RelativeRangeValueInput from './RelativeRangeValueInput';
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

const getValue = (fieldName, value: number | null, unsetRangeValue, previousRangeType?: moment.unitOfTime.DurationConstructor) => RELATIVE_RANGE_TYPES.map(({ type }) => {
  const unsetRange = value === unsetRangeValue;
  const diff = moment.duration(value, 'seconds').as(type);
  const valueInputIsEmpty = value === null;

  if (valueInputIsEmpty) {
    return {
      rangeValue: value,
      rangeType: previousRangeType ?? type,
      unsetRange: false,
      [fieldName]: value,
    };
  }

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
  unsetRangeValue: number | undefined
  title: string,
  defaultRange: number
  disableUnsetRange?: boolean
}

const useSyncInputsWithFormState = ({ rangeValue, rangeType, value, fieldName, unsetRangeValue, setInputValue }) => {
  useEffect(() => {
    if (rangeValue !== value) {
      setInputValue(getValue(fieldName, value, unsetRangeValue, rangeType));
    }
  }, [rangeValue, rangeType, value, fieldName, unsetRangeValue, setInputValue]);
};

const RelativeRangeSelectInner = ({
  value,
  onChange,
  name,
  disabled,
  unsetRangeLabel,
  error,
  defaultRange,
  title,
  disableUnsetRange,
  unsetRangeValue,
  fieldName,
  limitDuration,
}: Required<Props> & {
  value: number | null,
  onChange: (changeEvent: { target: { name: string, value: number | null } }) => void,
  name: string,
  error: string | undefined,
}) => {
  const { initialValues } = useFormikContext<TimeRangeDropDownFormValues>();
  const availableRangeTypes = buildRangeTypes(limitDuration);
  const [inputValue, setInputValue] = useState(getValue(fieldName, value, unsetRangeValue));

  useSyncInputsWithFormState({
    rangeValue: inputValue.rangeValue, rangeType: inputValue.rangeType, value, fieldName, unsetRangeValue, setInputValue,
  });

  const _onChange = React.useCallback((nextValue) => {
    onChange({ target: { name, value: nextValue } });
  }, [name, onChange]);

  const _onChangeTime = React.useCallback((event) => {
    const inputIsEmpty = event.target.value === '';
    const newTimeValue = inputIsEmpty ? null : moment.duration(event.target.value || 1, inputValue.rangeType).asSeconds();

    _onChange(newTimeValue);
  }, [_onChange, inputValue.rangeType]);

  const _onChangeType = (type) => {
    const newTimeValue = moment.duration(inputValue.rangeValue || 1, type).asSeconds();

    _onChange(newTimeValue);
  };

  const _onUnsetRange = (event) => {
    const hasInitialRelativeRange = isTypeRelative(initialValues.nextTimeRange);
    const _defaultRange = (
      hasInitialRelativeRange
      && fieldName in initialValues.nextTimeRange
      && initialValues.nextTimeRange[fieldName]
    ) ? initialValues.nextTimeRange[fieldName] : defaultRange;

    _onChange(event.target.checked ? unsetRangeValue : _defaultRange);
  };

  const _onSetPreset = (range) => {
    const newFromValue = getValue(fieldName, range, unsetRangeValue);

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
        <RelativeRangeValueInput disabled={disabled}
                                 error={error}
                                 fieldName={fieldName}
                                 onChange={_onChangeTime}
                                 unsetRange={inputValue.unsetRange}
                                 value={inputValue.rangeValue} />
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
        <ConfiguredRelativeTimeRangeSelector onChange={_onSetPreset} disabled={disabled} />
      </ConfiguredWrapper>
    </RangeWrapper>
  );
};

const RelativeRangeSelect = ({ disabled, fieldName, limitDuration, unsetRangeLabel, defaultRange, title, disableUnsetRange, unsetRangeValue }: Props) => (
  <Field name={`nextTimeRange.${fieldName}`}>
    {({ field: { value, onChange, name }, meta: { error } }) => (
      <RelativeRangeSelectInner defaultRange={defaultRange}
                                disabled={disabled}
                                disableUnsetRange={disableUnsetRange}
                                error={error}
                                fieldName={fieldName}
                                limitDuration={limitDuration}
                                name={name}
                                onChange={onChange}
                                title={title}
                                unsetRangeLabel={unsetRangeLabel}
                                unsetRangeValue={unsetRangeValue}
                                value={value} />
    )}
  </Field>
);

RelativeRangeSelect.defaultProps = {
  disabled: false,
  disableUnsetRange: false,
};

export default RelativeRangeSelect;
