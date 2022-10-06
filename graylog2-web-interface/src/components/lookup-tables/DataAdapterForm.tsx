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
import type { LookupTableAdapter, validationErrorsType } from 'logic/lookup-tables/types';
import { Col, Row } from 'components/bootstrap';
import { FormikFormGroup, TimeUnitInput, FormSubmit } from 'components/common';
import { LookupTableDataAdaptersActions } from 'stores/lookup-tables/LookupTableDataAdaptersStore';
import useScopePermissions from 'hooks/useScopePermissions';
import history from 'util/History';
import Routes from 'routing/Routes';

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
  const configRef = React.useRef(null);
  const [generateName, setGenerateName] = React.useState<boolean>(create);
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(dataAdapter);

  const plugin = usePluginEntities('lookupTableAdapters').find((p) => p.type === type);
  const pluginName = React.useMemo(() => (plugin.displayName || type), [plugin, type]);
  const DocComponent = React.useMemo(() => (plugin.documentationComponent), [plugin]);

  const sanitizeName = (inName: string) => {
    return inName.trim().replace(/\W+/g, '-').toLocaleLowerCase();
  };

  const handleTitleChange = (values: LookupTableAdapter, setValues: any) => (event: React.BaseSyntheticEvent) => {
    if (!generateName) return;
    const safeName = sanitizeName(event.target.value);

    setValues({
      ...values,
      title: event.target.value,
      name: safeName,
    });
  };

  const handleValidation = (values: LookupTableAdapter) => {
    const errors: any = {};

    if (!values.title) errors.title = 'Required';
    if (!values.name) errors.name = 'Required';
    if (values.name && values.title) validate(values);

    if (values.config.type !== 'none' && configRef.current && typeof configRef.current.validate !== 'undefined') {
      const confErrors = configRef.current?.validate() || {};
      if (!_.isEmpty(confErrors)) errors.config = confErrors;
    }

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

  const handleConfigChange = (event: React.BaseSyntheticEvent) => {
    console.log(event.target);
    console.log('is required:', event.target.required);
  };

  const getValidationMessage = React.useCallback((fieldName: string, defaultText: string) => {
    if (validationErrors[fieldName]) {
      return (
        <div>
          <span>{defaultText}</span><br />
          <span><b>{validationErrors[fieldName][0]}</b></span>
        </div>
      );
    }

    return <span>{defaultText}</span>;
  }, [validationErrors]);

  const getValidationState = React.useCallback((fieldName: string) => {
    return validationErrors[fieldName] ? 'error' : null;
  }, [validationErrors]);

  const configFieldSet = React.useMemo(() => {
    if (plugin) {
      return React.createElement(
        plugin.formComponent, {
          ref: configRef,
          config: dataAdapter.config,
          handleFormEvent: handleConfigChange,
          updateConfig: handleConfigChange,
          validationMessage: getValidationMessage,
          validationState: getValidationState,
          setDisableFormSubmission: (isDisabled: boolean) => (isDisabled),
        },
      );
    }

    return null;
  }, [plugin, dataAdapter.config, getValidationMessage, getValidationState]);

  const onCancel = () => history.push(Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.OVERVIEW);
  const updatable = !create && !loadingScopePermissions && scopePermissions?.is_mutable;

  return (
    <>
      <Title title={title} typeName={pluginName} create={create} />
      <Row>
        <Col lg={6}>
          <Formik initialValues={{ ...INIT_DATA_ADAPTER, ...dataAdapter }}
                  validate={handleValidation}
                  validateOnBlur
                  validateOnChange={false}
                  validateOnMount={!create}
                  onSubmit={handleSubmit}
                  enableReinitialize>
            {({ errors, values, setValues, isSubmitting }) => (
              <Form className="form form-horizontal">
                <fieldset>
                  <FormikFormGroup type="text"
                                   name="title"
                                   label="Title"
                                   autoFocus
                                   required
                                   help={errors.title ? null : 'A short title for this data adapter.'}
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
                                   error={validationErrors.name ? validationErrors.name[0] : null}
                                   onChange={() => setGenerateName(false)}
                                   help={
                                     (errors.name || validationErrors.name)
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
                {configFieldSet}
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
