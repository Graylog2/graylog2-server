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

import type { Attribute } from 'stores/PaginationTypes';
import useUserDateTime from 'hooks/useUserDateTime';
import AbsoluteDateInput from 'views/components/searchbar/date-time-picker/AbsoluteDateInput';
import { ModalSubmit } from 'components/common';
import { Checkbox } from 'components/bootstrap';

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
  margin: 0 0 10px 0;
`);

const Sections = styled.div`
  margin-bottom: 10px;
`;

const Section = styled.div`
  :not(:last-child) {
    margin-bottom: 10px;
  }
`;

const SectionHeader = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
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

const DateConfiguration = ({ name: fieldName, label, allTimeLabel }: {
  name: string,
  label: string,
  allTimeLabel: string
}) => {
  const { formatTime } = useUserDateTime();

  return (
    <Field name={fieldName}>
      {({ field: { value, onChange, name }, meta: { error } }) => {
        const _onChange = (newValue) => onChange({ target: { name, value: newValue } });
        const onChangeAllTime = () => onChange({ target: { name, value: value ? undefined : formatTime(new Date()) } });

        return (
          <div>
            <SectionHeader>
              <b>{label}</b>
              <StyledCheckbox onChange={onChangeAllTime} checked={!value}>{allTimeLabel}</StyledCheckbox>
            </SectionHeader>
            <AbsoluteDateInput name="from"
                               value={value}
                               disabled={!value}
                               onChange={_onChange} />
            {error && <ErrorMessage>{error}</ErrorMessage>}
          </div>
        );
      }}
    </Field>
  );
};

type Props = {
  attribute: Attribute,
  onSubmit: (filter: { title: string, value: string }) => void,
  initialValues?: FormValues
  scenario: 'create' | 'edit',
}

const DateRangeForm = ({ attribute, onSubmit, initialValues, scenario }: Props) => {
  const { userTimezone, formatTime } = useUserDateTime();

  const _onSubmit = (formValues: FormValues) => {
    onSubmit({
      title: `${formValues.from || 'All time'} - ${formValues.until || 'Now'}`,
      value: `${formValues.from || ''}><${formValues.until || ''}`,
    });
  };

  const _initialValues = initialValues ?? {
    from: formatTime(moment().subtract(5, 'minutes')),
    until: undefined,
  };

  return (
    <Container>
      <Formik initialValues={_initialValues} onSubmit={_onSubmit}>
        <Form>
          <Sections>
            <Section>
              <DateConfiguration name="from" label="From" allTimeLabel="All time" />
            </Section>
            <Section>
              <DateConfiguration name="until" label="Until" allTimeLabel="Now" />
            </Section>
          </Sections>
          <Info>
            Format: <DateTimeFormat>YYYY-MM-DD [HH:mm:ss[.SSS]]</DateTimeFormat>.<br />
            All timezones using: <b>{userTimezone}</b>.
          </Info>
          <ModalSubmit submitButtonText={`${scenario === 'create' ? 'Create' : 'Update'} filter`}
                       bsSize="small"
                       displayCancel={false} />
        </Form>
      </Formik>
    </Container>
  );
};

DateRangeForm.defaultProps = {
  initialValues: undefined,
};

export default DateRangeForm;
