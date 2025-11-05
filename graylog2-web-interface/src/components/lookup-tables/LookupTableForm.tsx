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
import styled from 'styled-components';
import { Formik, Form } from 'formik';
import type { FormikProps } from 'formik';
import _omit from 'lodash/omit';
import type { LookupTable } from 'src/logic/lookup-tables/types';

import { FormSubmit } from 'components/common';
import { CachesContainer, CachePicker, DataAdaptersContainer, DataAdapterPicker } from 'components/lookup-tables';
import useScopePermissions from 'hooks/useScopePermissions';
import { useCreateLookupTable, useUpdateLookupTable } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import LookupTableFormFields from 'components/lookup-tables/LookupTableFormFields';

export type LookupTableType = LookupTable & {
  enable_single_value: boolean;
  enable_multi_value: boolean;
};

export const INIT_TABLE_VALUES: LookupTableType = {
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
      {({ isSubmitting }) => (
        <Form className="form form-horizontal">
          <LookupTableFormFields />

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
