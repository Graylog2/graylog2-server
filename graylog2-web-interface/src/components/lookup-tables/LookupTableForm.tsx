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
import { Formik, Form } from 'formik';
import type { FormikProps } from 'formik';
import _omit from 'lodash/omit';
import type { LookupTable } from 'src/logic/lookup-tables/types';

import { Input } from 'components/bootstrap';
import { FormikFormGroup, JSONValueInput, FormSubmit } from 'components/common';
import { CachesContainer, CachePicker, DataAdaptersContainer, DataAdapterPicker } from 'components/lookup-tables';
import useScopePermissions from 'hooks/useScopePermissions';
import { useCreateLookupTable, useUpdateLookupTable } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

type LookupTableType = LookupTable & {
  enable_single_value: boolean;
  enable_multi_value: boolean;
};

const INIT_TABLE_VALUES: LookupTableType = {
  id: undefined,
  title: '',
  description: '',
  name: '',
  cache_id: undefined,
  data_adapter_id: undefined,
  enable_single_value: false,
  default_single_value: '',
  default_single_value_type: 'NULL',
  enable_multi_value: false,
  default_multi_value: '',
  default_multi_value_type: 'NULL',
  content_pack: null,
};

type Props = {
  onClose: () => void;
  onCacheCreateClick: () => void;
  onDataAdapterCreateClick: () => void;
  create?: boolean;
  table?: LookupTableType;
  dataAdapter?: string;
  cache?: string;
};

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

const StyledFormSubmitWrapper = styled.div`
  width: 100%;
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
`;

const LookupTableForm = ({
  onClose,
  onCacheCreateClick,
  onDataAdapterCreateClick,
  create = true,
  table = INIT_TABLE_VALUES,
  dataAdapter = '',
  cache = '',
}: Props) => {
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(table);
  const { createLookupTable, creatingLookupTable } = useCreateLookupTable();
  const { updateLookupTable, updatingLookupTable } = useUpdateLookupTable();
  const sendTelemetry = useSendTelemetry();
  const formikRef = React.useRef<FormikProps<LookupTableType>>(null);

  const validate = (values: LookupTableType) => {
    const errors = {};
    const requiredFields: (keyof LookupTableType)[] = [
      'title',
      'name',
      'cache_id',
      'data_adapter_id',
      'cache_id',
      'default_single_value',
      'default_multi_value',
    ];

    requiredFields.forEach((requiredField) => {
      if (!values[requiredField]) {
        if (requiredField === 'default_single_value' && !values.enable_single_value) return;
        if (requiredField === 'default_multi_value' && !values.enable_multi_value) return;
        errors[requiredField] = 'Required';
      }
    });

    return errors;
  };

  const handleSubmit = (values: LookupTableType) => {
    const valuesToSave: LookupTable = _omit(values, ['enable_single_value', 'enable_multi_value']);

    const promise = create ? createLookupTable(valuesToSave) : updateLookupTable(valuesToSave);

    return promise.then(() => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.LUT[create ? 'CREATED' : 'UPDATED'], {
        app_pathname: 'lut',
        app_section: 'lut',
      });

      onClose();
    });
  };

  const initialValues: LookupTableType = {
    ...INIT_TABLE_VALUES,
    ...table,
    data_adapter_id: table?.data_adapter_id || dataAdapter || undefined,
    cache_id: table?.cache_id || cache || undefined,
    enable_single_value: table.default_single_value !== '',
    enable_multi_value: table.default_multi_value !== '',
  };

  const updatable = !create && !loadingScopePermissions && scopePermissions?.is_mutable;

  React.useEffect(() => {
    if (formikRef.current) {
      if (dataAdapter) {
        formikRef.current.setFieldValue('data_adapter_id', dataAdapter);
      }

      if (cache) {
        formikRef.current.setFieldValue('cache_id', cache);
      }
    }
  }, [cache, dataAdapter]);

  return (
    <Formik
      initialValues={initialValues}
      validate={validate}
      onSubmit={async (values, formikHelpers) => {
        const errors = await formikHelpers.validateForm();

        if (Object.keys(errors).length === 0) {
          return handleSubmit(values);
        }

        return Promise.resolve();
      }}
      innerRef={formikRef}>
      {({ values, errors, touched, setFieldValue, setFieldTouched, setValues, isSubmitting }) => (
        <Form className="form form-horizontal">
          <fieldset>
            <FormikFormGroup
              type="text"
              name="title"
              label="Title *"
              help={touched.title && errors.title ? undefined : 'A short title for this lookup table.'}
              labelClassName="d-block mb-1"
              wrapperClassName="d-block"
              formGroupClassName="mb-3"
            />

            <FormikFormGroup
              type="text"
              name="description"
              label="Description"
              help="Description of the lookup table."
              labelClassName="d-block mb-1"
              wrapperClassName="d-block"
              formGroupClassName="mb-3"
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
                checked={values.enable_single_value}
                onChange={() => {
                  setFieldValue('enable_single_value', !values.enable_single_value);

                  if (values.enable_single_value) {
                    setFieldValue('default_single_value', '');
                    setFieldValue('default_single_value_type', 'NULL');
                  }
                }}
              />
              {values.enable_single_value && (
                <StyledJSONValueInput
                  label="Default single value *"
                  help={
                    (touched.default_single_value && errors.default_single_value) ||
                    'The single value that is being used as lookup result if the data adapter or cache does not find a value.'
                  }
                  validationState={touched.default_single_value && errors.default_single_value ? 'error' : undefined}
                  onBlur={() => setFieldTouched('default_single_value', true)}
                  update={(value, valueType) => {
                    setValues({
                      ...values,
                      default_single_value: value,
                      default_single_value_type: valueType,
                    });
                  }}
                  value={values.default_single_value}
                  valueType={values.default_single_value_type || 'NULL'}
                  allowedTypes={['STRING', 'NUMBER', 'BOOLEAN', 'NULL']}
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
                checked={values.enable_multi_value}
                onChange={() => {
                  setFieldValue('enable_multi_value', !values.enable_multi_value);

                  if (values.enable_multi_value) {
                    setFieldValue('default_multi_value', '');
                    setFieldValue('default_multi_value_type', 'NULL');
                  }
                }}
              />
              {values.enable_multi_value && (
                <StyledJSONValueInput
                  label="Default multi value *"
                  help={
                    (touched.default_multi_value && errors.default_multi_value) ||
                    'The multi value that is being used as lookup result if the data adapter or cache does not find a value.'
                  }
                  validationState={touched.default_multi_value && errors.default_multi_value ? 'error' : undefined}
                  onBlur={() => setFieldTouched('default_multi_value', true)}
                  update={(value, valueType) => {
                    setValues({
                      ...values,
                      default_multi_value: value,
                      default_multi_value_type: valueType,
                    });
                  }}
                  value={values.default_multi_value}
                  valueType={values.default_multi_value_type || 'NULL'}
                  allowedTypes={['OBJECT', 'NULL']}
                />
              )}
            </StyledDefaultValueSection>
          </fieldset>

          <DataAdaptersContainer dataAdapter={dataAdapter}>
            <DataAdapterPicker onCreateClick={onDataAdapterCreateClick} />
          </DataAdaptersContainer>

          <CachesContainer cache={cache}>
            <CachePicker onCreateClick={onCacheCreateClick} />
          </CachesContainer>

          <fieldset>
            <StyledFormSubmitWrapper>
              {create && (
                <FormSubmit
                  submitButtonText="Create lookup table"
                  submitLoadingText="Creating lookup table..."
                  isSubmitting={isSubmitting || creatingLookupTable}
                  isAsyncSubmit
                  onCancel={onClose}
                />
              )}
              {updatable && (
                <FormSubmit
                  submitButtonText="Update lookup table"
                  submitLoadingText="Updating lookup table..."
                  isSubmitting={isSubmitting || updatingLookupTable}
                  isAsyncSubmit
                  onCancel={onClose}
                />
              )}
            </StyledFormSubmitWrapper>
          </fieldset>
        </Form>
      )}
    </Formik>
  );
};

export default LookupTableForm;
