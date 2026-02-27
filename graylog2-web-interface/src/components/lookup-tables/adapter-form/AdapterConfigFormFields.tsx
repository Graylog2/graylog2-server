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
import { useMemo } from 'react';
import { useFormikContext } from 'formik';
import type { FormikErrors } from 'formik';

import usePluginEntities from 'hooks/usePluginEntities';
import type { LookupTableAdapter } from 'logic/lookup-tables/types';

const validationMessageRenderer =
  (validationErrors: FormikErrors<LookupTableAdapter>) => (fieldName: string, defaultText: string) =>
    validationErrors[fieldName] ? (
      <div>
        <span>{defaultText}</span>
        &nbsp;
        <span>
          <b>{validationErrors[fieldName]}</b>
        </span>
      </div>
    ) : (
      <span>{defaultText}</span>
    );

type Props = {
  validationErrors: FormikErrors<LookupTableAdapter>;
};

const AdapterConfigFormFields = React.forwardRef(({ validationErrors }: Props, configRef: { current: any }) => {
  const {
    values: { config },
    setFieldValue,
  } = useFormikContext<LookupTableAdapter>();

  const adapterPlugins = usePluginEntities('lookupTableAdapters');
  const plugin = adapterPlugins.find((p) => p.type === config?.type);

  const validationState = (fieldName: string) => (validationErrors[fieldName] ? 'error' : null);
  const updateConfig = (newConfig: LookupTableAdapter['config']) => setFieldValue('config', newConfig);
  const handleFormEvent = (event: React.ChangeEvent<any>) => {
    const { name, value, type: typeFromTarget, checked } = event.target;
    const updatedValue = typeFromTarget === 'checkbox' ? checked : value;
    setFieldValue(`config.${name}`, updatedValue);
  };

  const ConfigForm = useMemo(() => plugin?.formComponent, [plugin]);

  if (!plugin) return null;

  return (
    <ConfigForm
      config={config}
      ref={configRef}
      validationMessage={validationMessageRenderer(validationErrors)}
      validationState={validationState}
      updateConfig={updateConfig}
      handleFormEvent={handleFormEvent}
      setFieldValue={setFieldValue}
    />
  );
});

export default AdapterConfigFormFields;
