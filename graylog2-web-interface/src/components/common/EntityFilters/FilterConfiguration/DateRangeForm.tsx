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
import React from 'react';
import styled, { css } from 'styled-components';
import { Formik, Form, Field } from 'formik';
import moment from 'moment/moment';

import useUserDateTime from 'hooks/useUserDateTime';
import AbsoluteDateInput from 'views/components/searchbar/time-range-filter/time-range-picker/AbsoluteDateInput';
import { ModalSubmit } from 'components/common';
import { Checkbox } from 'components/bootstrap';
import { isValidDate, toUTCFromTz, adjustFormat } from 'util/DateTime';
import { DATE_SEPARATOR, extractRangeFromString, timeRangeTitle } from 'components/common/EntityFilters/helpers/timeRange';

import type { Filter } from '../types';

type FormValues = {
  from: string | undefined,
  until: string | undefined,
}

const Container = styled.div`
  padding: 3px 10px;
  max-width: 250px;
`;

const Info = styled.p(({ theme }) => css`
  font-size: ${theme.fonts.size.small};
  margin: 0 0 10px;
`);

const Sections = styled.div`
  margin-bottom: 10px;
`;

const Section = styled.div`
  &:not(:last-child) {
    margin-bottom: 10px;
  }
`;

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

const DateTimeFormat = styled.code`
  padding: 0;
`;

const ErrorMessage = styled.span(({ theme }) => css`
  color: ${theme.colors.variant.dark.danger};
  font-size: ${theme.fonts.size.small};
  font-style: italic;
  padding: 3px 3px 9px;
  height: 1.5em;
`);

const DateConfiguration = ({ name: fieldName, label, checkboxLabel }: {
  name: string,
  label: string,
  checkboxLabel: string
}) => {
  const { formatTime } = useUserDateTime();

  return (
    <Field name={fieldName}>
      {({ field: { value, onChange, name }, meta: { error } }) => {
        const _onChange = (newValue: string) => onChange({ target: { name, value: newValue } });
        const onChangeAllTime = () => _onChange(value ? undefined : formatTime(new Date(), 'complete'));

        return (
          <div>
            <SectionHeader>
              <StyledLabel htmlFor={`date-input-${name}`}>{label}</StyledLabel>
              <StyledCheckbox onChange={onChangeAllTime} checked={!value}>{checkboxLabel}</StyledCheckbox>
            </SectionHeader>
            <AbsoluteDateInput name="from"
                               value={value}
                               disabled={value === undefined}
                               onChange={_onChange} />
            {error && <ErrorMessage>{error}</ErrorMessage>}
          </div>
        );
      }}
    </Field>
  );
};

const useInitialValues = (filter: Filter | undefined) => {
  const { formatTime } = useUserDateTime();

  if (filter) {
    const [from, until] = extractRangeFromString(filter.value);

    return ({
      from: from ? formatTime(from, 'complete') : undefined,
      until: until ? formatTime(until, 'complete') : undefined,
    });
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
      from?: string,
      until?: string,
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

type Props = {
  onSubmit: (filter: { title: string, value: string }) => void,
  filter: Filter | undefined,
}

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
            <Sections>
              <Section>
                <DateConfiguration name="from" label="From" checkboxLabel="All time" />
              </Section>
              <Section>
                <DateConfiguration name="until" label="Until" checkboxLabel="Now" />
              </Section>
            </Sections>
            <Info>
              Format: <DateTimeFormat>YYYY-MM-DD [HH:mm:ss[.SSS]]</DateTimeFormat>.<br />
              All timezones using: <b>{userTimezone}</b>.
            </Info>
            <ModalSubmit submitButtonText={`${filter ? 'Update' : 'Create'} filter`}
                         bsSize="small"
                         disabledSubmit={!isValid}
                         displayCancel={false} />
          </Form>
        )}
      </Formik>
    </Container>
  );
};

export default DateRangeForm;
