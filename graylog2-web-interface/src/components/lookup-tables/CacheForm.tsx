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
import { PluginStore } from 'graylog-web-plugin/plugin';
import type { LookupTableCache, validationErrorsType } from 'src/logic/lookup-tables/types';

import { Button, Col, Row } from 'components/bootstrap';
import { FormikFormGroup } from 'components/common';
import { LookupTableCachesActions } from 'stores/lookup-tables/LookupTableCachesStore';
import useScopePermissions from 'hooks/useScopePermissions';

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

const INIT_CACHE: LookupTableCache = {
  id: undefined,
  title: '',
  description: '',
  name: '',
  config: {},
};

type Props = {
  type: string,
  saved: () => void,
  title: string,
  create?: boolean,
  cache?: LookupTableCache,
  validate?: (arg: LookupTableCache) => void,
  validationErrors?: validationErrorsType,
};

const CacheForm = ({ type, saved, title, create, cache, validate, validationErrors }: Props) => {
  const configRef = React.useRef(null);
  const [generateName, setGenerateName] = React.useState<boolean>(create);
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(cache);

  const plugin = React.useMemo(() => {
    return PluginStore.exports('lookupTableCaches').find((p) => p.type === type);
  }, [type]);

  const pluginName = React.useMemo(() => (plugin.displayName || type), [plugin, type]);
  const DocComponent = React.useMemo(() => (plugin.documentationComponent), [plugin]);
  const configFieldSet = React.useMemo(() => {
    if (plugin) {
      return React.createElement(
        plugin.formComponent, { config: cache.config, ref: configRef },
      );
    }

    return null;
  }, [plugin, cache.config]);

  const sanitizeName = (inName: string) => {
    return inName.trim().replace(/\W+/g, '-').toLocaleLowerCase();
  };

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
      if (!_.isEmpty(confErrors)) errors.config = confErrors;
    }

    return errors;
  };

  const handleSubmit = (values: LookupTableCache) => {
    const promise = create
      ? LookupTableCachesActions.create(values)
      : LookupTableCachesActions.update(values);

    promise.then(() => { saved(); });
  };

  return (
    <>
      <Title title={title} typeName={pluginName} create={create} />
      <Row>
        <Col lg={6} style={{ marginTop: 10 }}>
          <Formik initialValues={{ ...INIT_CACHE, ...cache }}
                  validate={handleValidation}
                  validateOnBlur
                  validateOnChange={false}
                  validateOnMount={!create}
                  onSubmit={handleSubmit}
                  enableReinitialize>
            {({ errors, values, setValues }) => (
              <Form className="form form-horizontal">
                <fieldset>
                  <FormikFormGroup type="text"
                                   name="title"
                                   label="* Title"
                                   required
                                   help={errors.title ? null : 'A short title for this cache.'}
                                   onChange={handleTitleChange(values, setValues)}
                                   autoFocus
                                   labelClassName="col-sm-3"
                                   wrapperClassName="col-sm-9" />
                  <FormikFormGroup type="text"
                                   name="description"
                                   label="Description"
                                   help="Cache description."
                                   labelClassName="col-sm-3"
                                   wrapperClassName="col-sm-9" />
                  <FormikFormGroup type="text"
                                   name="name"
                                   label="* Name"
                                   required
                                   error={validationErrors.name ? validationErrors.name[0] : null}
                                   onChange={() => setGenerateName(false)}
                                   help={
                                    (errors.name || validationErrors.name)
                                      ? null
                                      : 'The name that is being used to refer to this cache. Must be unique.'
                                   }
                                   labelClassName="col-sm-3"
                                   wrapperClassName="col-sm-9" />

                </fieldset>
                {configFieldSet}
                <fieldset>
                  <Row>
                    <Col mdOffset={3} sm={12}>
                      {create ? (
                        <Button type="submit" bsStyle="success">Create Cache</Button>
                      ) : (!loadingScopePermissions && scopePermissions?.is_mutable) && (
                        <Button type="submit"
                                bsStyle="success"
                                role="button"
                                name="update">
                          Update Cache
                        </Button>
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

CacheForm.defaultProps = {
  create: true,
  cache: INIT_CACHE,
  validate: null,
  validationErrors: {},
};

export default CacheForm;
