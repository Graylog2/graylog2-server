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
import { useMemo } from 'react';
import { useFormikContext } from 'formik';
import styled, { css } from 'styled-components';

import useScopePermissions from 'hooks/useScopePermissions';
import { Input } from 'components/bootstrap';
import { FormikFormGroup, JSONValueInput } from 'components/common';
import type { LookupTable } from 'logic/lookup-tables/types';

import useDefaultValueValidation from './use-default-value-validation';

const StyledJSONValueInput = styled(JSONValueInput)`
  margin: 0;
  padding: 0 10px;
`;

const StyledDefaultValueSection = styled.div(
  ({ theme }) => css`
    border: 1px solid ${theme.colors.variant.lighter.default};
    border-radius: 10px;
    margin-bottom: ${theme.spacings.xxs};
    padding: 10px;
    margin-top: ${theme.spacings.md};
  `,
);

function LookupTableFormFields() {
  const { setValues, values, touched, errors } = useFormikContext<LookupTable>();
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(values);
  const { defaultValueErrors, validateDefaultValues, clearDefaultValueError } = useDefaultValueValidation();

  const canModify = useMemo(
    () => !values.id || (!loadingScopePermissions && scopePermissions?.is_mutable),
    [values.id, loadingScopePermissions, scopePermissions?.is_mutable],
  );

  // Formik validates each setFieldValue call against the values of the last render, so a handler that sets
  // several fields must use one setValues call, or the last validation runs on stale values and its errors stick.
  const updateValues = (changes: Partial<LookupTable>) => setValues((current) => ({ ...current, ...changes }));

  // Derived from the type rather than kept as separate state, so an empty value cannot hide a field that fails validation.
  const singleValueEnabled = !!values.default_single_value_type && values.default_single_value_type !== 'NULL';
  const multiValueEnabled = !!values.default_multi_value_type && values.default_multi_value_type !== 'NULL';

  return (
    <fieldset>
      <FormikFormGroup
        type="text"
        name="title"
        label="Title *"
        help={touched.title && errors.title ? undefined : 'A short title for this lookup table.'}
        labelClassName="d-block mb-1"
        wrapperClassName="d-block"
        formGroupClassName="mb-3"
        disabled={!canModify}
      />

      <FormikFormGroup
        type="text"
        name="description"
        label="Description"
        help="Description of the lookup table."
        labelClassName="d-block mb-1"
        wrapperClassName="d-block"
        formGroupClassName="mb-3"
        disabled={!canModify}
      />

      <FormikFormGroup
        type="text"
        name="name"
        label="Name *"
        help={
          touched.name && errors.name
            ? undefined
            : 'The name that is being used to refer to this lookup table. Must be unique.'
        }
        labelClassName="d-block mb-1"
        wrapperClassName="d-block"
        formGroupClassName="mb-3"
        disabled={!canModify}
      />

      <StyledDefaultValueSection>
        <Input
          id="enable_single_value"
          name="enable_single_value"
          type="checkbox"
          label="Enable single default value"
          help="Enable if the lookup table should provide a default for the single value."
          labelClassName="d-block mb-1"
          wrapperClassName="d-block"
          formGroupClassName="mb-3"
          disabled={!canModify}
          checked={singleValueEnabled}
          onChange={() => {
            clearDefaultValueError('default_single_value');
            updateValues(
              singleValueEnabled
                ? { default_single_value: '', default_single_value_type: 'NULL' }
                : { default_single_value_type: 'STRING' },
            );
          }}
        />
        {singleValueEnabled && (
          <StyledJSONValueInput
            label="Default single value *"
            help={
              defaultValueErrors.default_single_value ||
              'The single value that is being used as lookup result if the data adapter or cache does not find a value.'
            }
            validationState={defaultValueErrors.default_single_value ? 'error' : undefined}
            onBlur={() => validateDefaultValues()}
            update={(value, valueType) => {
              const changes = { default_single_value: value, default_single_value_type: valueType };

              clearDefaultValueError('default_single_value');
              updateValues(changes);
              // A type change can invalidate the value without the field ever being blurred.
              if (valueType !== values.default_single_value_type) validateDefaultValues({ ...values, ...changes });
            }}
            value={values.default_single_value}
            valueType={values.default_single_value_type}
            allowedTypes={['STRING', 'NUMBER', 'BOOLEAN']}
            disabled={!canModify}
          />
        )}
      </StyledDefaultValueSection>

      <StyledDefaultValueSection>
        <Input
          id="enable_multi_value"
          name="enable_multi_value"
          type="checkbox"
          label="Enable multi default value"
          help="Enable if the lookup table should provide a default for the multi value."
          labelClassName="d-block mb-1"
          wrapperClassName="d-block"
          formGroupClassName="mb-3"
          disabled={!canModify}
          checked={multiValueEnabled}
          onChange={() => {
            clearDefaultValueError('default_multi_value');
            updateValues(
              multiValueEnabled
                ? { default_multi_value: '', default_multi_value_type: 'NULL' }
                : { default_multi_value_type: 'OBJECT' },
            );
          }}
        />
        {multiValueEnabled && (
          <StyledJSONValueInput
            label="Default multi value *"
            help={
              defaultValueErrors.default_multi_value ||
              'The multi value that is being used as lookup result if the data adapter or cache does not find a value.'
            }
            validationState={defaultValueErrors.default_multi_value ? 'error' : undefined}
            onBlur={() => validateDefaultValues()}
            update={(value, valueType) => {
              const changes = { default_multi_value: value, default_multi_value_type: valueType };

              clearDefaultValueError('default_multi_value');
              updateValues(changes);
              // A type change can invalidate the value without the field ever being blurred.
              if (valueType !== values.default_multi_value_type) validateDefaultValues({ ...values, ...changes });
            }}
            value={values.default_multi_value}
            valueType={values.default_multi_value_type}
            allowedTypes={['OBJECT']}
            disabled={!canModify}
          />
        )}
      </StyledDefaultValueSection>
    </fieldset>
  );
}

export default LookupTableFormFields;
