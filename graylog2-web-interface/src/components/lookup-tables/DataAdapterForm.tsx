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
import _ from 'lodash';
import { Formik, Form } from 'formik';

import usePluginEntities from 'hooks/usePluginEntities';
import type { LookupTableAdapter, LookupTableDataAdapterConfig, validationErrorsType } from 'logic/lookup-tables/types';
import { Col, Row } from 'components/bootstrap';
import { FormikFormGroup, TimeUnitInput, FormSubmit } from 'components/common';
import { LookupTableDataAdaptersActions } from 'stores/lookup-tables/LookupTableDataAdaptersStore';
import useScopePermissions from 'hooks/useScopePermissions';
import history from 'util/History';
import Routes from 'routing/Routes';

import AdapterConfigFieldset from './adapters/AdapterConfigFieldset';

type TitleProps = {
  title: string,
  typeName: string,
  create: boolean,
};

const Title = ({ title, typeName, create }: TitleProps) => {
  const TagName = create ? 'h3' : 'h2';

  return (
    <TagName style={{ marginBottom: '12px' }}>
      {title} <small>({typeName})</small>
    </TagName>
  );
};

const INIT_DATA_ADAPTER: LookupTableAdapter = {
  id: undefined,
  title: '',
  description: '',
  name: '',
  custom_error_ttl_enabled: false,
  custom_error_ttl: null,
  custom_error_ttl_unit: null,
  config: { type: 'none' },
};

type Props = {
  type: string,
  title: string,
  saved: () => void,
  create?: boolean,
  dataAdapter?: LookupTableAdapter,
  validate?: (arg: LookupTableAdapter) => void,
  validationErrors?: validationErrorsType,
};

const DataAdapterForm = ({ type, title, saved, create, dataAdapter, validate, validationErrors }: Props) => {
  const [generateName, setGenerateName] = React.useState<boolean>(create);

  const [formErrors, setFormErrors] = React.useState(_.cloneDeep(validationErrors));
  React.useEffect(() => setFormErrors(_.cloneDeep(validationErrors)), [validationErrors]);

  const { loadingScopePermissions, scopePermissions } = useScopePermissions(dataAdapter);

  const plugin = usePluginEntities('lookupTableAdapters').find((p) => p.type === type);
  const pluginName = React.useMemo(() => (plugin.displayName || type), [plugin, type]);
  const DocComponent = React.useMemo(() => (plugin.documentationComponent), [plugin]);

  const sanitizeName = (inName: string) => {
    return inName.trim().replace(/\W+/g, '-').toLocaleLowerCase();
  };

  const handleTitleChange = (
    values: LookupTableAdapter,
    setValues: (arg: LookupTableAdapter) => void,
  ) => (event: React.BaseSyntheticEvent) => {
    if (!generateName) return;
    const safeName = sanitizeName(event.target.value);

    setValues({
      ...values,
      title: event.target.value,
      name: safeName,
    });
  };

  const validateForm = (values: LookupTableAdapter) => {
    const errors: validationErrorsType = {};
    const requiredFieldNames = [...document.querySelectorAll('input:required')].map((input: HTMLInputElement) => input.name);

    requiredFieldNames.forEach((name: string) => {
      if (name in values && !values[name]) errors[name] = ['Required'];
      if (name in values.config && !values.config[name]) errors[name] = ['Required'];
    });

    if (Object.keys(errors).length === 0) validate(values);
    setFormErrors(errors);

    return errors;
  };

  const handleTTLUpdate = (values: LookupTableAdapter, setValues: any) => (value: number, unit: string, enabled: boolean) => {
    setValues({
      ...values,
      custom_error_ttl: value,
      custom_error_ttl_unit: unit,
      custom_error_ttl_enabled: enabled,
    });
  };

  const handleSubmit = (values: LookupTableAdapter) => {
    const promise = create
      ? LookupTableDataAdaptersActions.create(values)
      : LookupTableDataAdaptersActions.update(values);

    promise.then(() => saved());
  };

  const handleConfigInputChange = (values: LookupTableAdapter, setValues: any) => (event: React.BaseSyntheticEvent) => {
    const newValues = { ...values };
    newValues.config[event.target.name] = event.target.type === 'checkbox' ? event.target.checked : event.target.value;

    validateForm(newValues);
    setValues(newValues);
  };

  const handleTimeUnitInputChange = (values: LookupTableAdapter, setValues: any) => (newConfig: LookupTableDataAdapterConfig) => {
    const newValues = { ...values, config: newConfig };
    validateForm(newValues);
    setValues(newValues);
  };

  const getValidationMessage = (fieldName: string, defaultText: string) => {
    if (formErrors[fieldName]) {
      return (
        <div>
          <span>{defaultText}</span>&nbsp;<span><b>{formErrors[fieldName][0]}</b></span>
        </div>
      );
    }

    return <span>{defaultText}</span>;
  };

  const getValidationState = (fieldName: string) => {
    return formErrors[fieldName] ? 'error' : null;
  };

  const onCancel = () => history.push(Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.OVERVIEW);
  const updatable = !create && !loadingScopePermissions && scopePermissions?.is_mutable;

  return (
    <>
      <Title title={title} typeName={pluginName} create={create} />
      <Row>
        <Col lg={6}>
          <Formik initialValues={dataAdapter}
                  validate={validateForm}
                  validateOnMount={!create}
                  onSubmit={handleSubmit}
                  enableReinitialize>
            {({ values, setValues, isSubmitting }) => (
              <Form className="form form-horizontal">
                <fieldset>
                  <FormikFormGroup type="text"
                                   name="title"
                                   label="Title"
                                   autoFocus
                                   required
                                   help={formErrors.title ? null : 'A short title for this data adapter.'}
                                   onChange={handleTitleChange(values, setValues)}
                                   labelClassName="col-sm-3"
                                   wrapperClassName="col-sm-9" />
                  <FormikFormGroup type="text"
                                   name="description"
                                   label="Description"
                                   help="Data adapter description."
                                   labelClassName="col-sm-3"
                                   wrapperClassName="col-sm-9" />
                  <FormikFormGroup type="text"
                                   name="name"
                                   label="Name"
                                   required
                                   error={formErrors.name ? formErrors.name[0] : null}
                                   onChange={() => setGenerateName(false)}
                                   help={
                                     (formErrors.name)
                                       ? null
                                       : 'The name that is being used to refer to this data adapter. Must be unique.'
                                   }
                                   labelClassName="col-sm-3"
                                   wrapperClassName="col-sm-9" />
                  <TimeUnitInput label="Custom Error TTL"
                                 help="Define a custom TTL for caching erroneous results. Otherwise the default of 5 seconds is used"
                                 update={handleTTLUpdate(values, setValues)}
                                 name="custom_error_ttl"
                                 unitName="custom_error_ttl_unit"
                                 value={values.custom_error_ttl}
                                 unit={values.custom_error_ttl_unit || 'MINUTES'}
                                 units={['MILLISECONDS', 'SECONDS', 'MINUTES', 'HOURS', 'DAYS']}
                                 enabled={values.custom_error_ttl_enabled}
                                 labelClassName="col-sm-3"
                                 wrapperClassName="col-sm-9" />
                </fieldset>
                <AdapterConfigFieldset formComponent={plugin.formComponent}
                                       config={values.config}
                                       handleFormEvent={handleConfigInputChange(values, setValues)}
                                       updateConfig={handleTimeUnitInputChange(values, setValues)}
                                       validationMessage={getValidationMessage}
                                       validationState={getValidationState}
                                       setDisableFormSubmission={false} />
                <fieldset>
                  <Row>
                    <Col mdOffset={3} sm={12}>
                      {create && (
                        <FormSubmit submitButtonText="Create adapter"
                                    submitLoadingText="Creating adapter..."
                                    isSubmitting={isSubmitting}
                                    isAsyncSubmit
                                    onCancel={onCancel} />
                      )}
                      {updatable && (
                        <FormSubmit submitButtonText="Update adapter"
                                    submitLoadingText="Updating adapter..."
                                    isAsyncSubmit
                                    isSubmitting={isSubmitting}
                                    onCancel={onCancel} />
                      )}
                    </Col>
                  </Row>
                </fieldset>
              </Form>
            )}
          </Formik>
        </Col>
        <Col lg={6} style={{ marginTop: 10 }}>{DocComponent ? <DocComponent /> : null}</Col>
      </Row>
    </>
  );
};

DataAdapterForm.defaultProps = {
  create: true,
  dataAdapter: INIT_DATA_ADAPTER,
  validate: null,
  validationErrors: {},
};

export default DataAdapterForm;
