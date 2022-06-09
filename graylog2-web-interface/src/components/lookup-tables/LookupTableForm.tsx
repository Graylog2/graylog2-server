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
import { Formik, Field, Form } from 'formik';
import type { FieldProps } from 'formik';

import { Col, Row, Button, Input } from 'components/bootstrap';
import { JSONValueInput } from 'components/common';
import { CachesContainer, CachePicker, DataAdaptersContainer, DataAdapterPicker } from 'components/lookup-tables';
import { LookupTablesActions } from 'stores/lookup-tables/LookupTablesStore';
import type { LookupTable } from 'logic/lookup-tables/types';

interface ITable {
  id: string|number|undefined,
  title: string,
  description: string,
  name: string,
  cache_id: string|undefined,
  data_adapter_id: string|undefined,
  enable_single_value: boolean,
  default_single_value: string,
  default_single_value_type: 'STRING'|'NUMBER'|'BOOLEAN'|'NULL',
  enable_multi_value: boolean,
  default_multi_value: string,
  default_multi_value_type: 'OBJECT'|'NULL',
}

const defaultTableValues: ITable = {
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
};

interface LookupTableFormProps {
    saved: () => void,
    create: boolean,
    table: ITable,
}

const LookupTableForm: React.FC<LookupTableFormProps> = ({
  saved,
  create = true,
  table = defaultTableValues,
}) => {
  const validate = (values: ITable) => {
    const errors = {};
    const requiredFields: (keyof ITable)[] = ['title', 'description', 'name', 'cache_id', 'data_adapter_id'];

    requiredFields.forEach((requiredField) => {
      if (!values[requiredField]) {
        errors[requiredField] = 'Required';
      }
    });

    return errors;
  };

  const handleSubmit = (values: ITable) => {
    let promise;
    const { enable_single_value, enable_multi_value, ...valuesToSave } = values;

    if (create) {
      promise = LookupTablesActions.create(valuesToSave as LookupTable);
    } else {
      promise = LookupTablesActions.update(valuesToSave as LookupTable);
    }

    promise.then(() => {
      saved();
    });
  };

  const initialValues = {
    ...defaultTableValues,
    ...table,
    enable_single_value: (table.default_single_value !== '') && (table.default_single_value_type !== 'NULL'),
    enable_multi_value: (table.default_multi_value !== '') && (table.default_multi_value_type !== 'NULL'),
  };

  return (
    <Formik initialValues={initialValues}
            validate={validate}
            enableReinitialize
            onSubmit={async (values, formikHelpers) => {
              const errors = await formikHelpers.validateForm();

              if (Object.keys(errors).length === 0) {
                handleSubmit(values);
              }
            }}>
      {({ values, errors, touched, setFieldValue }) => (
        <Form className="form form-horizontal">
          <fieldset>
            <Field name="title">
              {({ field }: FieldProps) => (
                <Input type="text"
                       id="title"
                       label="Title"
                       autoFocus
                       bsStyle={(touched.title && errors.title) ? 'error' : undefined}
                       help={(touched.title && errors.title) || 'A short title for this lookup table.'}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9"
                       {...field} />
              )}
            </Field>

            <Field name="description">
              {({ field }: FieldProps) => (
                <Input type="text"
                       id="description"
                       label="Description"
                       bsStyle={(touched.description && errors.description) ? 'error' : undefined}
                       help={(touched.description && errors.description) || 'Description of the lookup table.'}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9"
                       {...field} />
              )}
            </Field>

            <Field name="name">
              {({ field }: FieldProps) => (
                <Input type="text"
                       id="name"
                       label="Name"
                       bsStyle={(touched.name && errors.name) ? 'error' : undefined}
                       help={(touched.name && errors.name) || 'The name that is being used to refer to this lookup table. Must be unique.'}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9"
                       {...field} />
              )}
            </Field>

            <Input id="enable_single_value"
                   name="enable_single_value"
                   type="checkbox"
                   label="Enable single default value"
                   help="Enable if the lookup table should provide a default for the single value."
                   wrapperClassName="col-md-offset-3 col-md-9"
                   checked={values.enable_single_value}
                   onChange={() => {
                     setFieldValue('enable_single_value', !values.enable_single_value);

                     if (values.enable_single_value) {
                       setFieldValue('default_single_value', '');
                       setFieldValue('default_single_value_type', 'NULL');
                     }
                   }} />

            {values.enable_single_value
            && (
            <JSONValueInput label="Default single value"
                            help="The single value that is being used as lookup result if the data adapter or cache does not find a value."
                            update={(value, valueType) => {
                              setFieldValue('default_single_value', value);
                              setFieldValue('default_single_value_type', valueType);
                            }}
                            value={values.default_single_value}
                            valueType={values.default_single_value_type || 'NULL'}
                            allowedTypes={['STRING', 'NUMBER', 'BOOLEAN', 'NULL']}
                            labelClassName="col-sm-3"
                            wrapperClassName="col-sm-9" />
            )}

            <Input id="enable_multi_value"
                   name="enable_multi_value"
                   type="checkbox"
                   label="Enable multi default value"
                   help="Enable if the lookup table should provide a default for the multi value."
                   wrapperClassName="col-md-offset-3 col-md-9"
                   checked={values.enable_multi_value}
                   onChange={() => {
                     setFieldValue('enable_multi_value', !values.enable_multi_value);

                     if (values.enable_multi_value) {
                       setFieldValue('default_multi_value', '');
                       setFieldValue('default_multi_value_type', 'NULL');
                     }
                   }} />
            {values.enable_multi_value
            && (
            <JSONValueInput label="Default multi value"
                            help="The multi value that is being used as lookup result if the data adapter or cache does not find a value."
                            update={(value, valueType) => {
                              setFieldValue('default_multi_value', value);
                              setFieldValue('default_multi_value_type', valueType);
                            }}
                            value={values.default_multi_value}
                            valueType={values.default_multi_value_type || 'NULL'}
                            allowedTypes={['OBJECT', 'NULL']}
                            labelClassName="col-sm-3"
                            wrapperClassName="col-sm-9" />
            )}
          </fieldset>

          <DataAdaptersContainer>
            <DataAdapterPicker name="data_adapter_id" />
          </DataAdaptersContainer>

          <CachesContainer>
            <CachePicker name="cache_id" />
          </CachesContainer>

          <fieldset>
            <Row>
              <Col mdOffset={3} md={9}>
                <Button type="submit" bsStyle="success">{create ? 'Create Lookup Table' : 'Update Lookup Table'}</Button>
              </Col>
            </Row>
          </fieldset>
        </Form>
      )}
    </Formik>
  );
};

export default LookupTableForm;
