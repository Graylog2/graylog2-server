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
import { useFormikContext } from 'formik';

import usePluginEntities from 'hooks/usePluginEntities';
import type { LookupTableCache } from 'logic/lookup-tables/types';

const CacheConfigFormFields = React.forwardRef((_p, configRef: { current: any }) => {
  const {
    values: { config },
  } = useFormikContext<LookupTableCache>();

  const cachePlugins = usePluginEntities('lookupTableAdapters');
  const plugin = cachePlugins.find((p) => p.type === config?.type);

  const ConfigForm = React.useMemo(() => plugin?.formComponent, [plugin]);

  if (!plugin) return null;

  return <ConfigForm config={config} ref={configRef} />;
});

export default CacheConfigFormFields;
