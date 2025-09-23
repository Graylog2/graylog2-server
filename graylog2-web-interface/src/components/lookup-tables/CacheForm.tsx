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
import isEmpty from 'lodash/isEmpty';
import { Formik, Form } from 'formik';
import { PluginStore } from 'graylog-web-plugin/plugin';
import type { LookupTableCache, validationErrorsType } from 'src/logic/lookup-tables/types';

import { Col, Row } from 'components/bootstrap';
import { FormikFormGroup, FormSubmit } from 'components/common';
import { useCreateCache, useUpdateCache } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import useScopePermissions from 'hooks/useScopePermissions';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

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

const INIT_CACHE: LookupTableCache = {
  id: undefined,
  title: '',
  description: '',
  name: '',
  config: {},
};

type Props = {
  type: string;
  saved: (response: any) => void;
  title: string;
  onCancel: () => void;
  create?: boolean;
  cache?: LookupTableCache;
  validate?: (arg: LookupTableCache) => void;
  validationErrors?: validationErrorsType;
};

const CacheForm = ({
  type,
  saved,
  title,
  onCancel,
  create = true,
  cache = INIT_CACHE,
  validate = null,
  validationErrors = {},
}: Props) => {
  const configRef = React.useRef(null);
  const [generateName, setGenerateName] = React.useState<boolean>(create);
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(cache);
  const { createCache, creatingCache } = useCreateCache();
  const { updateCache, updatingCache } = useUpdateCache();
  const sendTelemetry = useSendTelemetry();

  const plugin = React.useMemo(() => PluginStore.exports('lookupTableCaches').find((p) => p.type === type), [type]);

  const pluginName = React.useMemo(() => plugin.displayName || type, [plugin, type]);
  const DocComponent = React.useMemo(() => plugin.documentationComponent, [plugin]);
  const configFieldSet = React.useMemo(() => {
    if (plugin) {
      return React.createElement(plugin.formComponent, { config: cache.config, ref: configRef });
    }

    return null;
  }, [plugin, cache.config]);

  const sanitizeName = (inName: string) => inName.trim().replace(/\W+/g, '-').toLocaleLowerCase();

  const handleTitleChange = (values: LookupTableCache, setValues: any) => (event: React.BaseSyntheticEvent) => {
    if (!generateName) return;
    const safeName = sanitizeName(event.target.value);

    setValues({
      ...values,
      title: event.target.value,
      name: safeName,
    });
  };

  const handleValidation = (values: LookupTableCache) => {
    const errors: any = {};

    if (!values.title) errors.title = 'Required';

    if (!values.name) {
      errors.name = 'Required';
    } else {
      validate(values);
    }

    if (values.config.type !== 'none') {
      const confErrors = configRef.current?.validate() || {};
      if (!isEmpty(confErrors)) errors.config = confErrors;
    }

    return errors;
  };

  const handleSubmit = (values: LookupTableCache) => {
    const promise = create ? createCache(values) : updateCache(values);

    return promise.then((response) => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.LUT[create ? 'CACHE_CREATED' : 'CACHE_UPDATED'], {
        app_pathname: 'lut',
        app_section: 'lut_cache',
        event_details: {
          type: values?.config?.type,
        },
      });

      saved(response);
    });
  };

  const updatable = !create && !loadingScopePermissions && scopePermissions?.is_mutable;

  return (
    <>
      <Title title={title} typeName={pluginName} create={create} />
      <Formik
        initialValues={{ ...INIT_CACHE, ...cache }}
        validate={handleValidation}
        validateOnChange
        validateOnMount={!create}
        onSubmit={handleSubmit}
        enableReinitialize>
        {({ errors, values, setValues, isSubmitting }) => (
          <Form className="form form-horizontal">
            <Row>
              <Col lg={6} style={{ marginTop: 10 }}>
                <fieldset>
                  <FormikFormGroup
                    type="text"
                    name="title"
                    label="* Title"
                    required
                    help={errors.title ? null : 'A short title for this cache.'}
                    onChange={handleTitleChange(values, setValues)}
                    autoFocus
                    labelClassName="col-sm-3"
                    wrapperClassName="col-sm-9"
                  />
                  <FormikFormGroup
                    type="text"
                    name="description"
                    label="Description"
                    help="Cache description."
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
                        : 'The name that is being used to refer to this cache. Must be unique.'
                    }
                    labelClassName="col-sm-3"
                    wrapperClassName="col-sm-9"
                  />
                </fieldset>
                {configFieldSet}
              </Col>
              <Col lg={6} style={{ marginTop: 10 }}>
                {DocComponent ? <DocComponent /> : null}
              </Col>
            </Row>
            <Row style={{ marginBottom: 20 }}>
              <Col md={3} mdOffset={9}>
                {create && (
                  <FormSubmit
                    submitButtonText="Create cache"
                    submitLoadingText="Creating cache..."
                    isSubmitting={isSubmitting || creatingCache}
                    isAsyncSubmit
                    onCancel={onCancel}
                  />
                )}
                {updatable && (
                  <FormSubmit
                    submitButtonText="Update cache"
                    submitLoadingText="Updating cache..."
                    isAsyncSubmit
                    isSubmitting={isSubmitting || updatingCache}
                    onCancel={onCancel}
                  />
                )}
              </Col>
            </Row>
          </Form>
        )}
      </Formik>
    </>
  );
};

export default CacheForm;
