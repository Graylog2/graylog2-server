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
import { useCallback, useEffect, useRef, useState } from 'react';
import { useFormikContext } from 'formik';

import { useValidateCache } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import { FormikFormGroup } from 'components/common';
import type { LookupTableCache, validationErrorsType } from 'logic/lookup-tables/types';

import CacheConfigFormFields from './CacheConfigFormFields';

function CacheFormFields() {
  const { errors, values, touched, setValues, setErrors } = useFormikContext<LookupTableCache>();
  const { validateCache } = useValidateCache();
  const configRef = useRef(null);
  const [generateName, setGenerateName] = useState<boolean>(!values.title);

  const _sanitizeName = (inName: string) => inName.trim().replace(/\W+/g, '-').toLocaleLowerCase();

  const _runValidations = useCallback(() => {
    validateCache(values).then((resp: { errors: validationErrorsType }) => {
      const auxErrors = Object.keys(resp.errors).reduce((acc, key) => {
        // eslint-disable-next-line no-param-reassign
        if (errors[key]) acc[key] = errors[key];
        // eslint-disable-next-line no-param-reassign
        else acc[key] = resp.errors[key][0];

        return acc;
      }, {});

      const configErrors = configRef?.current?.validate?.() || {};
      setErrors({ ...auxErrors, ...configErrors });
    });
  }, [validateCache, values, setErrors, errors]);

  const handleTitleChange = (event: React.BaseSyntheticEvent) => {
    if (!generateName) return;
    const safeName = _sanitizeName(event.target.value);

    setValues({
      ...values,
      title: event.target.value,
      name: safeName,
    });
  };

  useEffect(() => _runValidations(), [_runValidations, values]);

  return (
    <fieldset>
      <FormikFormGroup
        type="text"
        name="title"
        label="* Title"
        required
        help={touched.title && errors.title ? null : 'A short title for this cache.'}
        onChange={handleTitleChange}
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
        error={touched.name && errors.name ? errors.name : null}
        onChange={() => setGenerateName(!touched.name)}
        help={
          touched.name && errors.name ? null : 'The name that is being used to refer to this cache. Must be unique.'
        }
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-9"
      />
      <CacheConfigFormFields ref={configRef} validationErrors={errors} />
    </fieldset>
  );
}

export default CacheFormFields;
