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
import { useCallback } from 'react';
import styled, { css } from 'styled-components';
import { Formik, Form, useField } from 'formik';
import moment from 'moment/moment';

import useUserDateTime from 'hooks/useUserDateTime';
import { ModalSubmit, Icon } from 'components/common';
import { Checkbox } from 'components/bootstrap';
import { isValidDate, toUTCFromTz, adjustFormat } from 'util/DateTime';
import {
  DATE_SEPARATOR,
  extractRangeFromString,
  timeRangeTitle,
} from 'components/common/EntityFilters/helpers/timeRange';
import DateTimePicker from 'views/components/searchbar/time-range-filter/time-range-picker/DateTimePicker';
import StringUtils from 'util/StringUtils';

import type { Filter } from '../types';

type FormValues = {
  from: string | undefined;
  until: string | undefined;
};

const Container = styled.div`
  padding: 3px 10px;
  max-width: fit-content;
`;

const Info = styled.p(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.small};
    margin: 0 0 10px;
  `,
);

const SectionHeader = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 3px;
`;

const StyledLabel = styled.label`
  margin: 0;
`;

const StyledCheckbox = styled(Checkbox)`
  &.checkbox {
    margin: 0;
  }
`;

const useInitialValues = (filter: Filter | undefined) => {
  const { formatTime } = useUserDateTime();

  if (filter) {
    const [from, until] = extractRangeFromString(filter.value);

    return {
      from: from ? formatTime(from, 'complete') : undefined,
      until: until ? formatTime(until, 'complete') : undefined,
    };
  }

  return {
    from: formatTime(moment().subtract(5, 'minutes'), 'complete'),
    until: undefined,
  };
};

const formatError = 'Format must be: YYYY-MM-DD [HH:mm:ss[.SSS]].';
const rangeError = 'The "Until" date must come after the "From" date.';

const validate = (values: FormValues) => {
  let errors: {
    from?: string;
    until?: string;
  } = {};

  if (values.from && !isValidDate(values.from)) {
    errors = { ...errors, from: formatError };
  }

  if (values.until && !isValidDate(values.until)) {
    errors = { ...errors, until: formatError };
  }

  if (values.from >= values.until) {
    errors = { ...errors, until: rangeError };
  }

  if (values.from === undefined && values.until === undefined) {
    errors = { ...errors, from: 'Remove filter to search from "All time" until "Now".' };
  }

  return errors;
};

const PickerContainer = styled.div`
  display: flex;
  align-items: center;
  flex-direction: row;
  gap: 10px;
`;

const PickerWrap = styled.div`
  max-width: 240px;
`;

type PickerProps = { name: 'from' | 'until' };
const Picker = ({ name }: PickerProps) => {
  const { formatTime } = useUserDateTime();
  const label = StringUtils.capitalizeFirstLetter(name);
  const [{ onChange, value }, meta] = useField(name);
  const _onChange = useCallback(
    (newValue: string) => onChange({ target: { name, value: newValue } }),
    [onChange, name],
  );
  const onChangeAllTime = () => _onChange(value ? undefined : formatTime(new Date(), 'complete'));
  const checkboxLabel = name === 'from' ? 'All Time' : 'Now';
  const isChecked = !value;

  return (
    <PickerWrap>
      <SectionHeader>
        <StyledLabel htmlFor={`date-input-${name}`}>{label}</StyledLabel>
        <StyledCheckbox onChange={onChangeAllTime} checked={isChecked}>
          {checkboxLabel}
        </StyledCheckbox>
      </SectionHeader>
      <DateTimePicker disabled={isChecked} error={meta.error} onChange={_onChange} value={value} range={label} />
    </PickerWrap>
  );
};
const FromPicker = () => <Picker name="from" />;
const UntilPicker = () => <Picker name="until" />;

type Props = {
  onSubmit: (filter: { title: string; value: string }) => void;
  filter: Filter | undefined;
};

const DateRangeForm = ({ filter, onSubmit }: Props) => {
  const { userTimezone } = useUserDateTime();
  const initialValues = useInitialValues(filter);

  const _onSubmit = (formValues: FormValues) => {
    const toInternalTime = (date: string) => adjustFormat(toUTCFromTz(date, userTimezone), 'internal');
    const utcFrom = formValues.from ? toInternalTime(formValues.from) : '';
    const utcUntil = formValues.until ? toInternalTime(formValues.until) : '';

    onSubmit({
      title: timeRangeTitle(formValues.from, formValues.until),
      value: `${utcFrom}${DATE_SEPARATOR}${utcUntil}`,
    });
  };

  return (
    <Container data-testid="time-range-form">
      <Formik initialValues={initialValues} onSubmit={_onSubmit} validate={validate}>
        {({ isValid }) => (
          <Form>
            <PickerContainer>
              <FromPicker />

              <Icon name="arrow_right_alt" />

              <UntilPicker />
            </PickerContainer>
            <Info>
              All timezones using: <b>{userTimezone}</b>.
            </Info>
            <ModalSubmit
              submitButtonText={`${filter ? 'Update' : 'Create'} filter`}
              bsSize="small"
              disabledSubmit={!isValid}
              displayCancel={false}
            />
          </Form>
        )}
      </Formik>
    </Container>
  );
};

export default DateRangeForm;
