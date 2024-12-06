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
import styled from 'styled-components';
import trim from 'lodash/trim';
import { Formik, Form } from 'formik';

import type { Filter } from 'components/common/EntityFilters/types';
import { ModalSubmit, FormikInput } from 'components/common';

type Props = {
  filter?: Filter,
  onSubmit: (filter: { title: string, value: string }, closeDropdown?: boolean) => void,
}

const FilterInput = styled(FormikInput)`
  margin-bottom: 5px;
`;
const Container = styled.div`
  padding: 3px 10px;
  max-width: 250px;
`;

type FormValues = {
  value: string | undefined,
}

const validate = ({ value }: FormValues) => {
  if (!value || trim(value) === '') {
    return { value: 'Must not be empty.' };
  }

  return {};
};

const GenericFilterInput = ({ filter = undefined, onSubmit }: Props) => {
  const initialValues = { value: filter?.value };
  const createFilter = useCallback(({ value }: FormValues) => onSubmit({ title: value, value }, true), [onSubmit]);

  return (
    <Container data-testid="generic-filter-form">
      <Formik initialValues={initialValues} onSubmit={createFilter} validate={validate}>
        {({ isValid }) => (
          <Form>
            <FilterInput type="text"
                         id="generic-filters-input"
                         name="value"
                         formGroupClassName=""
                         required
                         placeholder="Enter value to filter for" />
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

export default GenericFilterInput;
