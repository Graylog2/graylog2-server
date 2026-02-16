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
import type { LookupTableCache } from 'logic/lookup-tables/types';

const validationMessageRenderer =
  (validationErrors: FormikErrors<LookupTableCache>) => (fieldName: string, defaultText: string) =>
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
  validationErrors: FormikErrors<LookupTableCache>;
};

const CacheConfigFormFields = React.forwardRef(({ validationErrors }: Props, configRef: { current: any }) => {
  const {
    values: { config },
  } = useFormikContext<LookupTableCache>();

  const cachePlugins = usePluginEntities('lookupTableCaches');
  const plugin = cachePlugins.find((p) => p.type === config?.type);

  const ConfigForm = useMemo(() => plugin?.formComponent, [plugin]);

  if (!plugin) return null;

  return <ConfigForm config={config} ref={configRef} validationMessage={validationMessageRenderer(validationErrors)} />;
});

export default CacheConfigFormFields;
