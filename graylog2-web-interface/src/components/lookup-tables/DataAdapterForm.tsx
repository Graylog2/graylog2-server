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
import isEmpty from 'lodash/isEmpty';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { FormikFormGroup, FormSubmit, TimeUnitInput } from 'components/common';
import { Col, Row } from 'components/bootstrap';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { useCreateAdapter, useUpdateAdapter } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { LookupTableAdapter, validationErrorsType } from 'logic/lookup-tables/types';

type TitleProps = {
  title: string;
  typeName: string;
  create: boolean;
};

const Title = ({ title, typeName, create }: TitleProps) => {
  const TagName = create ? 'h3' : 'h2';

  return (
    <TagName style={{ marginBottom: '12px' }}>
      {title} <small>({typeName})</small>
    </TagName>
  );
};

type Props = {
  type: string;
  title: string;
  saved: (response: any) => void;
  onCancel: () => void;
  create?: boolean;
  dataAdapter?: LookupTableAdapter;
  validate?: (arg: LookupTableAdapter) => void;
  validationErrors?: validationErrorsType;
};

const INIT_ADAPTER = {
  id: undefined,
  title: '',
  description: '',
  name: '',
  custom_error_ttl_enabled: false,
  custom_error_ttl: null,
  custom_error_ttl_unit: null,
  config: {},
};

const DataAdapterForm = ({
  type,
  title,
  saved,
  onCancel,
  create = true,
  dataAdapter = INIT_ADAPTER,
  validate = null,
  validationErrors = {},
}: Props) => {
  const sendTelemetry = useSendTelemetry();
  const configRef = React.useRef(null);
  const [generateName, setGenerateName] = React.useState<boolean>(create);
  const [configReady, setConfigReady] = React.useState(false);
  const { createAdapter, creatingAdapter } = useCreateAdapter();
  const { updateAdapter, updatingAdapter } = useUpdateAdapter();

  React.useEffect(() => {
    setConfigReady(false);
  }, [type]);

  const plugin = React.useMemo(() => PluginStore.exports('lookupTableAdapters').find((p) => p.type === type), [type]);

  const validationState = (fieldName) => {
    if (validationErrors[fieldName]) {
      return 'error' as const;
    }

    return null;
  };

  const validationMessage = (fieldName, defaultText) => {
    if (validationErrors[fieldName]) {
      return (
        <div>
          <span>{defaultText}</span>
          &nbsp;
          <span>
            <b>{validationErrors[fieldName][0]}</b>
          </span>
        </div>
      );
    }

    return <span>{defaultText}</span>;
  };

  const DocComponent = React.useMemo(() => plugin.documentationComponent, [plugin]);
  const pluginDisplayName = React.useMemo(() => plugin.displayName || type, [plugin, type]);

  const sanitizeName = (inName: string) => inName.trim().replace(/\W+/g, '-').toLocaleLowerCase();

  const handleTitleChange = (values: LookupTableAdapter, setValues: any) => (event: React.BaseSyntheticEvent) => {
    if (!generateName) return;
    const safeName = sanitizeName(event.target.value);

    setValues({
      ...values,
      title: event.target.value,
      name: safeName,
    });
  };

  const updateCustomErrorTTL = (
    value: number,
    enabled: boolean,
    unit: string,
    values: LookupTableAdapter,
    setValues: any,
  ) => {
    setValues({
      ...values,
      custom_error_ttl: value,
      custom_error_ttl_enabled: enabled,
      custom_error_ttl_unit: unit,
    });
  };

  const handleValidation = (values: LookupTableAdapter) => {
    const errors: any = {};

    if (!values.title) errors.title = 'Required';

    if (!values.name) {
      errors.name = 'Required';
    } else {
      validate(values);
    }

    if (values.config?.type !== 'none' && configReady) {
      const confErrors = configRef.current?.validate?.() || {};
      if (!isEmpty(confErrors)) errors.config = confErrors;
    }

    return errors;
  };

  const handleSubmit = (values: LookupTableAdapter) => {
    const promise = create ? createAdapter(values) : updateAdapter(values);

    return promise.then((response) => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.LUT[create ? 'DATA_ADAPTER_CREATED' : 'DATA_ADAPTER_UPDATED'], {
        app_pathname: 'lut',
        app_section: 'lut_data_adapter',
        event_details: {
          type: dataAdapter?.config?.type,
        },
      });

      saved(response);
    });
  };

  const isLDAP = dataAdapter.config.type === 'LDAP' && dataAdapter.config?.user_passwd?.is_set;
  const configWithOptionalPassword = {
    ...dataAdapter.config,
    ...(isLDAP ? { user_passwd: { is_set: true, keep_value: true } } : {}),
  };

  return (
    <>
      <Title title={title} typeName={pluginDisplayName} create={create} />
      <Formik
        initialValues={{
          ...INIT_ADAPTER,
          ...dataAdapter,
          config: configWithOptionalPassword,
        }}
        validate={handleValidation}
        validateOnChange
        onSubmit={handleSubmit}
        enableReinitialize>
        {({ errors, values, setValues, setFieldValue, isSubmitting }) => {
          const configFieldSet =
            plugin &&
            React.createElement(plugin.formComponent, {
              config: values.config,
              validationMessage,
              validationState,
              updateConfig: (newConfig) => setFieldValue('config', newConfig),
              handleFormEvent: (event) => {
                const { name, value, type: typeFromTarget, checked } = event.target;
                const updatedValue = typeFromTarget === 'checkbox' ? checked : value;

                setFieldValue(`config.${name}`, updatedValue);
              },
              setFieldValue,
              ref: (ref) => {
                configRef.current = ref;
                setConfigReady(true);
              },
            });

          return (
            <Form className="form form-horizontal">
              <Row>
                <Col lg={6} style={{ marginTop: 10 }}>
                  <fieldset>
                    <FormikFormGroup
                      type="text"
                      name="title"
                      label="* Title"
                      required
                      help={errors.title ? null : 'A short title for this data adapter.'}
                      onChange={handleTitleChange(values, setValues)}
                      autoFocus
                      labelClassName="col-sm-3"
                      wrapperClassName="col-sm-9"
                    />
                    <FormikFormGroup
                      type="text"
                      name="description"
                      label="Description"
                      help="Data adapter description."
                      labelClassName="col-sm-3"
                      wrapperClassName="col-sm-9"
                    />
                    <FormikFormGroup
                      type="text"
                      name="name"
                      label="* Name"
                      required
                      error={validationErrors.name ? validationErrors.name[0] : null}
                      onChange={() => setGenerateName(false)}
                      help={
                        errors.name || validationErrors.name
                          ? null
                          : 'The name that is being used to refer to this data adapter. Must be unique.'
                      }
                      labelClassName="col-sm-3"
                      wrapperClassName="col-sm-9"
                    />
                    <TimeUnitInput
                      label="Custom Error TTL"
                      help="Define a custom TTL for caching erroneous results. Otherwise the default of 5 seconds is used"
                      update={(value, unit, enabled) => updateCustomErrorTTL(value, enabled, unit, values, setValues)}
                      value={values.custom_error_ttl}
                      unit={values.custom_error_ttl_unit || 'MINUTES'}
                      units={['MILLISECONDS', 'SECONDS', 'MINUTES', 'HOURS', 'DAYS']}
                      enabled={values.custom_error_ttl_enabled}
                      labelClassName="col-sm-3"
                      wrapperClassName="col-sm-9"
                    />
                    {configFieldSet}
                  </fieldset>
                </Col>
                <Col lg={6} style={{ marginTop: 10 }}>
                  {DocComponent ? <DocComponent /> : null}
                </Col>
              </Row>
              <Row style={{ marginBottom: 20 }}>
                <Col mdOffset={9} md={3}>
                  <FormSubmit
                    submitButtonText={create ? 'Create adapter' : 'Update adapter'}
                    disabledSubmit={isSubmitting || creatingAdapter || updatingAdapter}
                    onCancel={onCancel}
                  />
                </Col>
              </Row>
            </Form>
          );
        }}
      </Formik>
    </>
  );
};

export default DataAdapterForm;
