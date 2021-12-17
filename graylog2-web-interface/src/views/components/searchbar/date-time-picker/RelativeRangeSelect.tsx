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
import { Field, useFormikContext } from 'formik';
import styled, { css } from 'styled-components';
import moment from 'moment';

import { RELATIVE_RANGE_TYPES } from 'views/Constants';
import { Select } from 'components/common';
import type { RangeClassified } from 'views/components/searchbar/date-time-picker/types';

import { isTypeRelativeClassified, RELATIVE_CLASSIFIED_ALL_TIME_RANGE } from './RelativeTimeRangeClassifiedHelper';
import RelativeRangeValueInput from './RelativeRangeValueInput';
import type { TimeRangeDropDownFormValues } from './TimeRangeDropdown';

import TimerangeSelector from '../TimerangeSelector';
import RangePresetDropdown from '../RangePresetDropdown';

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

const buildRangeTypes = (limitDuration) => RELATIVE_RANGE_TYPES.map(({ label, type }) => {
  const typeDuration = moment.duration(1, type).asSeconds();

  if (limitDuration === 0 || typeDuration <= limitDuration) {
    return { label, value: type };
  }

  return null;
}).filter(Boolean);

type Props = {
  classifyRange: (range: number) => RangeClassified,
  defaultRange: RangeClassified
  disableUnsetRange?: boolean,
  disabled?: boolean,
  fieldName: 'range' | 'from' | 'to',
  limitDuration: number,
  onUnsetRange?: () => void,
  title: string,
  unsetRangeLabel: string,
  unsetRangeValue: number | undefined,
}

const RelativeRangeSelectInner = ({
  classifiedRange,
  classifyRange,
  defaultRange,
  disableUnsetRange,
  disabled,
  error,
  fieldName,
  limitDuration,
  name,
  onChange,
  onUnsetRange,
  title,
  unsetRangeLabel,
  unsetRangeValue,
}: Required<Props> & {
  classifiedRange: RangeClassified,
  error: string | undefined,
  name: string,
  onChange: (changeEvent: { target: { name: string, value: RangeClassified } }) => void,
}) => {
  const { initialValues } = useFormikContext<TimeRangeDropDownFormValues>();
  const availableRangeTypes = buildRangeTypes(limitDuration);
  const { isAllTime, value, unit } = classifiedRange;

  const _onChange = React.useCallback((newClassifiedRange) => {
    onChange({
      target: {
        name,
        value: newClassifiedRange,
      },
    });
  }, [name, onChange]);

  const _onChangeTime = React.useCallback((event) => {
    const inputIsEmpty = event.target.value === '';
    const inputValue = inputIsEmpty ? null : event.target.value;

    _onChange({
      value: inputValue,
      isAllTime,
      unit,
    });
  }, [_onChange, unit, isAllTime]);

  const _onChangeUnit = (newUnit) => {
    _onChange({ value, isAllTime, unit: newUnit });
  };

  const _onUnsetRange = (event) => {
    const isUnsetting = event.target.checked;
    const hasInitialRelativeRange = isTypeRelativeClassified(initialValues.nextTimeRange);
    const _defaultRange = (hasInitialRelativeRange && !initialValues.nextTimeRange[fieldName].isAllTime)
      ? initialValues.nextTimeRange[fieldName]
      : defaultRange;

    if (isUnsetting && !!onUnsetRange) {
      onUnsetRange();
    }

    _onChange(isUnsetting ? RELATIVE_CLASSIFIED_ALL_TIME_RANGE : _defaultRange);
  };

  const _onSetPreset = (range) => {
    const isUnsetting = range === 0;
    const newRange = isUnsetting ? unsetRangeValue : range;

    if (isUnsetting && !!onUnsetRange) {
      onUnsetRange();
    }

    _onChange(classifyRange(newRange));
  };

  return (
    <RangeWrapper>
      <RangeTitle>{title}</RangeTitle>
      <RangeCheck htmlFor={`relative-unset-${fieldName}`} className={disableUnsetRange && 'shortened'}>
        <input checked={isAllTime}
               className="mousetrap"
               disabled={disableUnsetRange}
               id={`relative-unset-${fieldName}`}
               onChange={_onUnsetRange}
               type="checkbox"
               value="0" />
        {unsetRangeLabel}
      </RangeCheck>
      <InputWrap>
        <RelativeRangeValueInput disabled={disabled}
                                 error={error}
                                 fieldName={fieldName}
                                 onChange={_onChangeTime}
                                 unsetRange={isAllTime}
                                 value={value} />
      </InputWrap>
      <StyledSelect clearable={false}
                    disabled={disabled || isAllTime}
                    id={`relative-timerange-${fieldName}-unit`}
                    inputProps={{ className: 'mousetrap' }}
                    name={`relative-timerange-${fieldName}-unit`}
                    onChange={_onChangeUnit}
                    options={availableRangeTypes}
                    placeholder="Select a range length"
                    value={unit} />

      <Ago />

      {error && (
        <ErrorMessage>
          {error}
        </ErrorMessage>
      )}

      <ConfiguredWrapper>
        <TimerangeSelector className="relative">
          <RangePresetDropdown disabled={disabled}
                               onChange={_onSetPreset} />
        </TimerangeSelector>
      </ConfiguredWrapper>
    </RangeWrapper>
  );
};

const RelativeRangeSelect = ({
  classifyRange,
  defaultRange,
  disableUnsetRange,
  disabled,
  fieldName,
  limitDuration,
  onUnsetRange,
  title,
  unsetRangeLabel,
  unsetRangeValue,
}: Props) => (
  <Field name={`nextTimeRange.${fieldName}`}>
    {({ field: { value, onChange, name }, meta: { error } }) => (
      <RelativeRangeSelectInner classifiedRange={value}
                                classifyRange={classifyRange}
                                defaultRange={defaultRange}
                                disableUnsetRange={disableUnsetRange}
                                disabled={disabled}
                                error={error}
                                fieldName={fieldName}
                                limitDuration={limitDuration}
                                name={name}
                                onUnsetRange={onUnsetRange}
                                onChange={onChange}
                                title={title}
                                unsetRangeLabel={unsetRangeLabel}
                                unsetRangeValue={unsetRangeValue} />
    )}
  </Field>
);

RelativeRangeSelect.defaultProps = {
  disabled: false,
  disableUnsetRange: false,
  onUnsetRange: undefined,
};

export default RelativeRangeSelect;
