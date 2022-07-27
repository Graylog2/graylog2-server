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
import { Formik, Form } from 'formik';
import PropTypes from 'prop-types';
import _omit from 'lodash/omit';
import type { LookupTable } from 'src/logic/lookup-tables/types';

import { LookupTablesActions } from 'stores/lookup-tables/LookupTablesStore';
import { Col, Row, Button, Input } from 'components/bootstrap';
import { FormikFormGroup, JSONValueInput } from 'components/common';
import { CachesContainer, CachePicker, DataAdaptersContainer, DataAdapterPicker } from 'components/lookup-tables';
import useScopePermissions from 'hooks/useScopePermissions';

type LookupTableType = LookupTable & {
  enable_single_value: boolean,
  enable_multi_value: boolean,
}

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
  saved: () => void,
  create: boolean,
  table: LookupTableType,
};

const LookupTableForm = ({ saved, create, table }: Props) => {
  const { getScopePermissions } = useScopePermissions();

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
    let promise: Promise<any>;

    const valuesToSave: LookupTable = _omit(values, ['enable_single_value', 'enable_multi_value']);

    if (create) {
      promise = LookupTablesActions.create(valuesToSave);
    } else {
      promise = LookupTablesActions.update(valuesToSave);
    }

    promise.then(() => {
      saved();
    });
  };

  const initialValues: LookupTableType = {
    ...INIT_TABLE_VALUES,
    ...table,
    enable_single_value: table.default_single_value !== '',
    enable_multi_value: table.default_multi_value !== '',
  };

  const showAction = (inTable: LookupTable, action: string): boolean => {
    // TODO: Update this method to check for the metadata
    const permissions = getPermissionsByScope(inTable._metadata?.scope);

    return permissions[action];
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
      {({ values, errors, touched, setFieldValue, setFieldTouched, setValues }) => (
        <Form className="form form-horizontal">
          <fieldset>
            <FormikFormGroup type="text"
                             name="title"
                             label="Title"
                             help={(touched.title && errors.title) ? undefined : 'A short title for this lookup table.'}
                             labelClassName="col-sm-3"
                             wrapperClassName="col-sm-9" />

            <FormikFormGroup type="text"
                             name="description"
                             label="Description"
                             help="Description of the lookup table."
                             labelClassName="col-sm-3"
                             wrapperClassName="col-sm-9" />

            <FormikFormGroup type="text"
                             name="name"
                             label="Name"
                             help={(touched.name && errors.name) ? undefined : 'The name that is being used to refer to this lookup table. Must be unique.'}
                             labelClassName="col-sm-3"
                             wrapperClassName="col-sm-9" />

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
            {values.enable_single_value && (
              <JSONValueInput label="Default single value"
                              help={(touched.default_single_value && errors.default_single_value) || 'The single value that is being used as lookup result if the data adapter or cache does not find a value.'}
                              validationState={(touched.default_single_value && errors.default_single_value) ? 'error' : undefined}
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
            {values.enable_multi_value && (
              <JSONValueInput label="Default multi value"
                              help={(touched.default_multi_value && errors.default_multi_value) || 'The multi value that is being used as lookup result if the data adapter or cache does not find a value.'}
                              validationState={(touched.default_multi_value && errors.default_multi_value) ? 'error' : undefined}
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
                              labelClassName="col-sm-3"
                              wrapperClassName="col-sm-9" />
            )}
          </fieldset>

          <DataAdaptersContainer>
            <DataAdapterPicker />
          </DataAdaptersContainer>

          <CachesContainer>
            <CachePicker />
          </CachesContainer>

          <fieldset>
            <Row>
              <Col mdOffset={3} md={9}>
                {create ? (
                  <Button type="submit" bsStyle="success">Create Lookup Table</Button>
                ) : showAction(table, 'edit') && (
                  <Button type="submit" bsStyle="success">Update Lookup Table</Button>
                )}
              </Col>
            </Row>
          </fieldset>
        </Form>
      )}
    </Formik>
  );
};

LookupTableForm.propTypes = {
  saved: PropTypes.func.isRequired,
  create: PropTypes.bool,
  table: PropTypes.object,
};

LookupTableForm.defaultProps = {
  create: true,
  table: INIT_TABLE_VALUES,
};

export default LookupTableForm;
